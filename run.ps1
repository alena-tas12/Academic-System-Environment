$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (!(Test-Path "lib\hsqldb.jar")) { throw "Missing lib\hsqldb.jar" }
New-Item -ItemType Directory -Force -Path "bin" | Out-Null
$files = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -cp "lib\hsqldb.jar" -d "bin" $files
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }
java -cp "bin;lib\hsqldb.jar" sms.runtime.SwingLauncher

