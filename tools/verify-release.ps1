param(
    [switch]$AllowDirty
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Checks = New-Object System.Collections.Generic.List[object]

function Add-Check {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Details
    )
    $Checks.Add([pscustomobject]@{
        Name = $Name
        Status = $Status
        Details = $Details
    }) | Out-Null
}

function Invoke-Step {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Name"
    $start = Get-Date
    Push-Location $WorkingDirectory
    try {
        & $Command
        if ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0) {
            throw "Command exited with code $LASTEXITCODE"
        }
        $elapsed = [math]::Round(((Get-Date) - $start).TotalSeconds, 1)
        Add-Check $Name "PASS" "${elapsed}s"
    } catch {
        Add-Check $Name "FAIL" $_.Exception.Message
        throw
    } finally {
        Pop-Location
    }
}

function Assert-CleanGit {
    if ($AllowDirty) {
        Add-Check "Git clean state" "SKIP" "AllowDirty enabled"
        return
    }

    $status = git -C $RepoRoot status --short
    if ($status) {
        Add-Check "Git clean state" "FAIL" "Working tree has uncommitted changes"
        $status | ForEach-Object { Write-Host $_ }
        throw "Working tree is dirty"
    }
    Add-Check "Git clean state" "PASS" "No uncommitted changes"
}

function Assert-NoTrackedGeneratedFiles {
    $tracked = git -C $RepoRoot ls-files ".env" ".env.production" "frontend/.next" "frontend/tsconfig.tsbuildinfo"
    if ($tracked) {
        Add-Check "Tracked generated/env files" "FAIL" ($tracked -join ", ")
        throw "Generated or env files are tracked"
    }
    Add-Check "Tracked generated/env files" "PASS" ".env, .env.production, .next, tsbuildinfo are not tracked"
}

function Assert-NoSecrets {
    $pattern = "AIza|sk_live_[A-Za-z0-9]{12,}|sk_test_[A-Za-z0-9]{12,}|whsec_[A-Za-z0-9]{12,}|spring\.datasource\.password=12345|gemini\.api\.key=AIza|APP_WEBHOOK_SECRET:-dev-secret|dev-webhook-secret-do-not-use-in-prod"
    $matches = & rg -n $pattern -S $RepoRoot `
        --glob "!frontend/node_modules/**" `
        --glob "!frontend/.next/**" `
        --glob "!tools/verify-release.ps1" `
        --glob "!**/target/**"

    if ($LASTEXITCODE -eq 0) {
        Add-Check "Secret scan" "FAIL" "Potential secret-like values found"
        $matches | ForEach-Object { Write-Host $_ }
        throw "Secret scan failed"
    }
    if ($LASTEXITCODE -ne 1) {
        Add-Check "Secret scan" "FAIL" "rg exited with code $LASTEXITCODE"
        throw "Secret scan command failed"
    }
    Add-Check "Secret scan" "PASS" "No real key patterns found"
}

try {
    Write-Host "Nhu Villas release verification"
    Write-Host "Repo: $RepoRoot"

    Assert-CleanGit
    Assert-NoTrackedGeneratedFiles
    Assert-NoSecrets

    Invoke-Step "Frontend lint" (Join-Path $RepoRoot "frontend") { npm run lint }
    Invoke-Step "Frontend typecheck" (Join-Path $RepoRoot "frontend") { npx tsc --noEmit }
    Invoke-Step "Frontend production build" (Join-Path $RepoRoot "frontend") { npm run build }
    Invoke-Step "Backend API tests" (Join-Path $RepoRoot "hotel") { .\mvnw.cmd test }
    Invoke-Step "Legacy compatibility tests" $RepoRoot { .\mvnw.cmd test }

    Write-Host ""
    Write-Host "Release verification summary"
    $Checks | Format-Table -AutoSize
    Write-Host "All release checks passed."
} catch {
    Write-Host ""
    Write-Host "Release verification failed."
    $Checks | Format-Table -AutoSize
    throw
}
