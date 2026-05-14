# Set environment variables for Android development
$javaCandidates = @(
	"D:\Tools\Java",
	"D:\Tools\Android\jbr",
	"C:\Program Files\Android\Android Studio\jbr"
)

$resolvedJavaHome = $javaCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $resolvedJavaHome) {
	Write-Error "No valid Java runtime found in known locations."
	return
}

$env:JAVA_HOME = $resolvedJavaHome
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"

Write-Host "Environment variables set:"
Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME = $env:GRADLE_USER_HOME"
Write-Host ""
Write-Host "You can now run: ./gradlew build"
