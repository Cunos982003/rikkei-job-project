$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $root 'docker-compose.prod.yml'
$envFile = Join-Path $root '.env.prod.example'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw 'Production Compose file is missing.'
}
if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Production environment example is missing.'
}

$rendered = & docker compose --env-file $envFile -f $composeFile config --format json 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Production Compose config failed: $($rendered -join [Environment]::NewLine)"
}
$config = ($rendered -join [Environment]::NewLine) | ConvertFrom-Json

$requiredServices = @(
    'postgres',
    'redis',
    'api-gateway',
    'auth-service',
    'profile-service',
    'job-service',
    'recruitment-service',
    'learning-service',
    'ai-career-service',
    'frontend'
)
$serviceNames = @($config.services.PSObject.Properties.Name)
foreach ($serviceName in $requiredServices) {
    if ($serviceName -notin $serviceNames) {
        throw "Required production service is missing: $serviceName"
    }
}

$forbiddenServices = @(
    'wiremock',
    'mock-api',
    'mailpit',
    'audit-service',
    'assessment-service',
    'evidence-service',
    'notification-service'
)
foreach ($serviceName in $forbiddenServices) {
    if ($serviceName -in $serviceNames) {
        throw "Out-of-scope service is included: $serviceName"
    }
}

$publishedPorts = @(
    foreach ($serviceName in $serviceNames) {
        foreach ($port in @($config.services.$serviceName.ports)) {
            if ($null -ne $port -and $null -ne $port.published) {
                [pscustomobject]@{
                    Service = $serviceName
                    Published = [string]$port.published
                    Target = [int]$port.target
                    HostIp = [string]$port.host_ip
                }
            }
        }
    }
)
if ($publishedPorts.Count -ne 1 -or $publishedPorts[0].Service -ne 'frontend') {
    throw 'Only the frontend may publish a host port.'
}
if ($publishedPorts[0].Target -ne 8080 -or $publishedPorts[0].HostIp -ne '127.0.0.1') {
    throw 'Frontend must publish container port 8080 on the loopback interface.'
}

$requiredVolumes = @('pgdata', 'profile-uploads', 'job-uploads', 'recruitment-uploads')
$declaredVolumes = @($config.volumes.PSObject.Properties.Name)
$mountedVolumes = @(
    foreach ($serviceName in $serviceNames) {
        foreach ($volume in @($config.services.$serviceName.volumes)) {
            if ($null -ne $volume.source) {
                [string]$volume.source
            }
        }
    }
)
foreach ($volumeName in $requiredVolumes) {
    if ($volumeName -notin $declaredVolumes -or $volumeName -notin $mountedVolumes) {
        throw "Expected persistent volume is not declared and mounted: $volumeName"
    }
}

$frontend = $config.services.frontend
if ($frontend.environment.PSObject.Properties.Name -contains 'LMS_SERVICE_KEY' -or
    $frontend.environment.PSObject.Properties.Name -contains 'LMS_BASE_URL') {
    throw 'Frontend must not receive the LMS service credentials.'
}

$profileEnvironment = $config.services.'profile-service'.environment
if ($profileEnvironment.APP_LMS_BASE_URL -ne 'https://lms.example.test' -or
    $profileEnvironment.APP_LMS_SERVICE_KEY -ne 'CHANGE_ME_LMS_SERVICE_KEY') {
    throw 'Profile service must receive the configured LMS URL and service key.'
}

$frontendTemplateMount = @($frontend.volumes | Where-Object {
    $_.target -eq '/etc/nginx/templates/default.conf.template'
})
$expectedTemplatePath = [IO.Path]::GetFullPath((Join-Path $root 'infra\docker\frontend.prod.conf'))
$actualTemplatePath = if ($frontendTemplateMount.Count -eq 1) {
    [IO.Path]::GetFullPath($frontendTemplateMount[0].source)
}
if ($frontendTemplateMount.Count -ne 1 -or $actualTemplatePath -ne $expectedTemplatePath) {
    throw 'Production frontend must use its production-specific Nginx template.'
}

$nginxConfig = Get-Content -LiteralPath (Join-Path $root 'infra\docker\frontend.prod.conf') -Raw
if ($nginxConfig -notmatch 'location \^~ /lms/\s*\{\s*return 404;\s*\}' -or
    $nginxConfig -match 'LMS_SERVICE_KEY|mock-api|proxy_pass\s+.*lms') {
    throw 'Production Nginx must block mock LMS ticket routes without forwarding the service key.'
}

$requiredVariables = @('LMS_BASE_URL', 'AI_PROVIDER_API_KEY')
foreach ($requiredVariable in $requiredVariables) {
    $temporaryEnvFile = Join-Path ([IO.Path]::GetTempPath()) "rikkei-prod-compose-$([Guid]::NewGuid().ToString('N')).env"
    try {
        $environmentLines = @(Get-Content -LiteralPath $envFile | Where-Object { $_ -notmatch "^$requiredVariable=" })
        [IO.File]::WriteAllLines($temporaryEnvFile, [string[]]$environmentLines, [Text.Encoding]::ASCII)
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $missingConfigOutput = & docker compose --env-file $temporaryEnvFile -f $composeFile config --quiet 2>&1
            $missingConfigExitCode = $LASTEXITCODE
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($missingConfigExitCode -eq 0) {
            throw "Compose accepted a deployment without required $requiredVariable."
        }
    }
    finally {
        if (Test-Path -LiteralPath $temporaryEnvFile) {
            Remove-Item -LiteralPath $temporaryEnvFile -Force
        }
    }
}

Write-Output 'Production Compose validation passed.'
