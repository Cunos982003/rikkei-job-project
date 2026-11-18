const fs = require('fs');
const path = require('path');

const SEED_DIR = path.resolve(__dirname, '..', 'infra', 'seed');
const FILES = [
  '01_seed_auth_db.sql',
  '02_seed_profile_db.sql',
  '03_seed_job_db.sql',
  '04_seed_recruitment_db.sql',
  '05_seed_learning_db.sql',
  '06_seed_assessment_db.sql',
  '07_seed_ai_db.sql',
  'run_all_seeds.sql'
];

let failed = 0;
let passed = 0;

function check(cond, label, detail = '') {
  if (cond) {
    console.log(`✅ [PASS] ${label}`);
    passed++;
  } else {
    console.error(`❌ [FAIL] ${label}${detail ? ' — ' + detail : ''}`);
    failed++;
  }
}

// 1. Check files exist & SQL quotes/parens balance
const contents = {};
for (const f of FILES) {
  const full = path.join(SEED_DIR, f);
  const exists = fs.existsSync(full);
  check(exists, `File exists: ${f}`);
  if (!exists) continue;
  const sql = fs.readFileSync(full, 'utf8');
  contents[f] = sql;

  // Strip SQL single-line comments before checking quote/paren balance
  const stripped = sql.split('\n').map(line => {
    let inQ = false;
    for (let i = 0; i < line.length; i++) {
      if (line[i] === "'") {
        if (inQ && line[i + 1] === "'") { i++; continue; }
        inQ = !inQ;
      } else if (!inQ && line[i] === '-' && line[i + 1] === '-') {
        return line.slice(0, i);
      }
    }
    return line;
  }).join('\n');

  let inQuote = false;
  let parenDepth = 0;
  for (let i = 0; i < stripped.length; i++) {
    const ch = stripped[i];
    if (ch === "'") {
      if (inQuote && stripped[i + 1] === "'") { i++; continue; }
      inQuote = !inQuote;
    } else if (!inQuote) {
      if (ch === '(') parenDepth++;
      else if (ch === ')') parenDepth--;
    }
  }
  check(!inQuote && parenDepth === 0, `SQL syntax balance (quotes & parens): ${f}`, `inQuote=${inQuote}, parenDepth=${parenDepth}`);
}

// 2. Validate every ::jsonb literal parses cleanly as valid JSON
for (const f of ['06_seed_assessment_db.sql', '07_seed_ai_db.sql']) {
  const sql = contents[f];
  const jsonbRegex = /'((?:[^']|'')*)'::jsonb/g;
  let match;
  let jsonCount = 0;
  let jsonErrors = 0;
  while ((match = jsonbRegex.exec(sql)) !== null) {
    jsonCount++;
    const raw = match[1].replace(/''/g, "'");
    try {
      JSON.parse(raw);
    } catch (e) {
      jsonErrors++;
      console.error(`   JSON parse error in ${f}: ${e.message} -> ${raw.slice(0, 80)}`);
    }
  }
  check(jsonCount > 0 && jsonErrors === 0, `Valid JSONB literals in ${f} (${jsonCount} checked)`);
}

// 3. Extract authoritative UUID sets
const authUsers = new Set();
const authUserCompanies = new Set();
{
  const re = /\('([0-9a-f-]{36})',\s*'([^']+)',\s*'([^']+)',\s*'([A-Z]+)',\s*'([^']+)',\s*(NULL|'[0-9a-f-]{36}')/g;
  let m;
  while ((m = re.exec(contents['01_seed_auth_db.sql'])) !== null) {
    authUsers.add(m[1]);
    if (m[6] !== 'NULL') authUserCompanies.add(m[6].replace(/'/g, ''));
  }
}
check(authUsers.size === 51, `Authoritative auth_db users count === 51 (got ${authUsers.size})`);

const companies = new Set();
const taxCodes = new Set();
{
  const block = contents['03_seed_job_db.sql'].split('-- 1. 10 Companies')[1].split('-- 2.')[0];
  const re = /\('([0-9a-f-]{36})',\s*'[^']+',\s*'(\d+)'/g;
  let m;
  let badTax = 0;
  while ((m = re.exec(block)) !== null) {
    companies.add(m[1]);
    if (m[2].length !== 10 && m[2].length !== 13) badTax++;
    taxCodes.add(m[2]);
  }
  check(companies.size === 10 && taxCodes.size === 10 && badTax === 0, `job_db companies === 10 with valid unique 10/13-digit tax codes`);
}

// Every company_id in auth_db exists in job_db.companies
let missingAuthComp = 0;
for (const cid of authUserCompanies) {
  if (!companies.has(cid)) missingAuthComp++;
}
check(missingAuthComp === 0, `All HR company_ids in clean_and_sync_users.sql exist in job_db.companies`);

// Extract jobs and statuses
const allJobs = new Map();
{
  const block = contents['03_seed_job_db.sql'].split('-- 4. 120 Jobs')[1].split('-- 5.')[0];
  const re = /\('(d0000000-0000-4000-8000-\d{12})',\s*'([0-9a-f-]{36})'.*?'(APPROVED|PENDING|REJECTED|EXPIRED)'/g;
  let m;
  while ((m = re.exec(block)) !== null) {
    allJobs.set(m[1], { companyId: m[2], status: m[3] });
  }
}
const statusCounts = { APPROVED: 0, PENDING: 0, REJECTED: 0, EXPIRED: 0 };
for (const j of allJobs.values()) statusCounts[j.status]++;
check(
  allJobs.size === 120 && statusCounts.APPROVED === 84 && statusCounts.PENDING === 12 && statusCounts.REJECTED === 12 && statusCounts.EXPIRED === 12,
  `job_db 120 jobs exact 70/10/10/10 split`,
  JSON.stringify(statusCounts)
);

// Check profile_db candidate_profiles all match auth_db users
{
  const block = contents['02_seed_profile_db.sql'].split('-- 3. 40 Candidate Profiles')[1].split('-- 4.')[0];
  const re = /\('([0-9a-f-]{36})',\s*'[^']+',\s*'\d{4}-\d{2}-\d{2}'/g;
  let m;
  let count = 0;
  let missing = 0;
  while ((m = re.exec(block)) !== null) {
    count++;
    if (!authUsers.has(m[1])) missing++;
  }
  check(count === 40 && missing === 0, `profile_db 40 candidate_profiles all match auth_db UUIDs (count=${count}, missing=${missing})`);
}

// Check recruitment_db applications reference valid authUsers, valid APPROVED jobs, valid companies, and integer status 0..6
{
  const block = contents['04_seed_recruitment_db.sql'].split('-- 1. 180 Recruitments')[1].split('-- 2.')[0];
  const re = /\('([0-9a-f-]{36})',\s*'(d0000000-0000-4000-8000-\d{12})',\s*'([0-9a-f-]{36})',\s*'([0-9a-f-]{36})',.*?'([^']+@[^']+)',\s*(\d),/g;
  let m;
  let count = 0, badUser = 0, badJob = 0, badComp = 0, badStatus = 0;
  while ((m = re.exec(block)) !== null) {
    count++;
    const [, , jobId, compId, userId, , stStr] = m;
    const st = Number(stStr);
    if (!authUsers.has(userId)) badUser++;
    const job = allJobs.get(jobId);
    if (!job || job.status !== 'APPROVED') badJob++;
    if (!companies.has(compId) || (job && job.companyId !== compId)) badComp++;
    if (st < 0 || st > 6) badStatus++;
  }
  check(
    count === 180 && badUser === 0 && badJob === 0 && badComp === 0 && badStatus === 0,
    `recruitment_db 180 applications valid FKs & ordinal status 0..6`,
    `count=${count}, badUser=${badUser}, badJob=${badJob}, badComp=${badComp}, badStatus=${badStatus}`
  );
}

// Check ai_db job_recommendations unique (user_id, job_id), valid user & job, 64-char input_hash
{
  const block = contents['07_seed_ai_db.sql'].split('-- 1. 160 Job Recommendations')[1].split('-- 2.')[0];
  const re = /\('([0-9a-f-]{36})',\s*'([0-9a-f-]{36})',\s*'(d0000000-0000-4000-8000-\d{12})',.*?'([0-9a-f]{64})'\)/g;
  let m;
  let count = 0, badUser = 0, badJob = 0;
  const pairs = new Set();
  while ((m = re.exec(block)) !== null) {
    count++;
    const [, , userId, jobId] = m;
    if (!authUsers.has(userId)) badUser++;
    if (!allJobs.has(jobId)) badJob++;
    pairs.add(`${userId}:${jobId}`);
  }
  check(
    count === 160 && pairs.size === 160 && badUser === 0 && badJob === 0,
    `ai_db 160 job_recommendations unique (user,job), valid FKs & 64-char SHA-256 input_hash`,
    `count=${count}, uniquePairs=${pairs.size}, badUser=${badUser}, badJob=${badJob}`
  );
}

// Check learning_db enrollments unique (user_id, lms_course_id) and valid authUsers
{
  const block = contents['05_seed_learning_db.sql'].split('INSERT INTO enrollments')[1].split('INSERT INTO learning_progress')[0];
  const re = /\('([0-9a-f-]{36})',\s*'([0-9a-f-]{36})',\s*'(CRS-\d+)'/g;
  let m;
  let count = 0, badUser = 0;
  const pairs = new Set();
  while ((m = re.exec(block)) !== null) {
    count++;
    if (!authUsers.has(m[2])) badUser++;
    pairs.add(`${m[2]}:${m[3]}`);
  }
  check(
    count === 68 && pairs.size === 68 && badUser === 0,
    `learning_db 68 enrollments unique (user_id, course_code) & valid authUsers`,
    `count=${count}, uniquePairs=${pairs.size}, badUser=${badUser}`
  );
}

// Check assessment_db attempts valid authUsers
{
  const block = contents['06_seed_assessment_db.sql'].split('INSERT INTO assessment_attempts')[1];
  const re = /\('([0-9a-f-]{36})',\s*'([0-9a-f-]{36})',\s*'([0-9a-f-]{36})',/g;
  let m;
  let count = 0, badUser = 0;
  while ((m = re.exec(block)) !== null) {
    count++;
    if (!authUsers.has(m[3])) badUser++;
  }
  check(
    count === 64 && badUser === 0,
    `assessment_db 64 attempts all reference valid auth_db users`,
    `count=${count}, badUser=${badUser}`
  );
}

console.log(`\n=== VERIFICATION RESULT: ${passed} PASS, ${failed} FAIL ===`);
if (failed > 0) process.exit(1);
