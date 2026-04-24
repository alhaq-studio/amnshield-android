# Set environment variables for Android development
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"

Write-Host "Environment variables set:"
Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "GRADLE_USER_HOME = $env:GRADLE_USER_HOME"
Write-Host ""
Write-Host "You can now run: ./gradlew build"
