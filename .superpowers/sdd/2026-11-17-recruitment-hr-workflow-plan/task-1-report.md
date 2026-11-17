# Task 1 Report: Recruitment round persistence and status policy

## Result

Implemented interview-round persistence and the immutable recruitment transition policy in Recruitment Service. No AI Career calls or unrelated scope changes were made. The approved transition map was confirmed in the workflow plan, so no unresolved ambiguity or security-sensitive concern blocked implementation.

## Changes

- Added `InterviewRoundResult` with `PENDING`, `PASS`, and `FAIL`.
- Added `InterviewRound`, mapped to `interview_rounds`:
  - `recruitment_id` is a non-null relation to `Recruitment`.
  - `round_no` is required and unique per recruitment.
  - `scheduled_at` and `score` are nullable.
  - `result` persists as an enum string and defaults to `PENDING`.
  - Database check restricts non-null scores to 0–100.
- Added the sorted `InterviewRoundRepository` query and the `Recruitment.interviewRounds` relation.
- Added Spring-managed `RecruitmentTransitionPolicy`; the immutable policy allows:
  - `SUBMITTED → SCREENING | REJECTED`
  - `SCREENING → INTERVIEW | REJECTED`
  - `INTERVIEW → REJECTED`
  - `OFFER → HIRED | REJECTED`
  - `HIRED`, `REJECTED`, and `WITHDRAWN` have no allowed outgoing transitions. In particular, `INTERVIEW → OFFER` is denied.
- Added tests for allowed and denied transitions, ordered round persistence, nullable schedule/score, inclusive score boundaries, out-of-range scores, and Hibernate schema update against an existing recruitment table.

## TDD and verification evidence

### RED

Ran from `backend\recruitment-service`:

```text
.\gradlew.bat test --tests "*RecruitmentTransitionPolicyTest" --tests "*InterviewRoundRepositoryTest"
```

Result: exit code 1 at `:compileTestJava`, with five `cannot find symbol` errors for the not-yet-created `InterviewRound`, `InterviewRoundResult`, `InterviewRoundRepository`, and `RecruitmentTransitionPolicy`. This was the expected failure before implementation.

### GREEN

The final focused run was:

```text
.\gradlew.bat test --tests "*RecruitmentTransitionPolicyTest" --tests "*InterviewRoundRepositoryTest" --tests "*RecruitmentRepositoryTest"
```

Result: `BUILD SUCCESSFUL` (17 seconds). The policy, round repository, and existing recruitment repository tests passed.

The existing-schema update check was also run independently:

```text
.\gradlew.bat test --tests "*InterviewRoundSchemaUpdateTest" --rerun-tasks
```

Result: `BUILD SUCCESSFUL` (16 seconds). Hibernate `ddl-auto: update` created `interview_rounds` while retaining a pre-created `recruitments` table.

The required complete suite was run from `backend\recruitment-service`:

```text
.\gradlew.bat test
```

Final result: `BUILD SUCCESSFUL` (15 seconds). Surefire reports contain **16 tests, 0 failures, 0 errors, 0 skipped**, across six suites:

| Suite | Tests | Failures / errors / skipped |
|---|---:|---:|
| `RecruitmentServiceApplicationTests` | 1 | 0 / 0 / 0 |
| `InterviewRoundRepositoryTest` | 3 | 0 / 0 / 0 |
| `InterviewRoundSchemaUpdateTest` | 1 | 0 / 0 / 0 |
| `RecruitmentRepositoryTest` | 1 | 0 / 0 / 0 |
| `RecruitmentServiceTest` | 8 | 0 / 0 / 0 |
| `RecruitmentTransitionPolicyTest` | 2 | 0 / 0 / 0 |

`git diff --check` passed.

### Other test-run details

- One interim focused run failed compilation because a test import was removed while its assertion still referenced `InterviewRoundResult`. The import was restored; the final focused and full runs above passed.
- Gradle emitted the existing deprecated-API note for `CvServiceClient.java`; the JVM emitted its class-data-sharing warning. Both runs completed successfully.

## Review and concerns

Self-review confirmed the next task can consume the entity, result enum, sorted repository query, and injectable `allows(from, to)` policy. The database-level score constraint and `(recruitment_id, round_no)` uniqueness constraint are present. No remaining concern identified.

## Commit

Subject: `feat(recruitment): add interview round persistence policy`
