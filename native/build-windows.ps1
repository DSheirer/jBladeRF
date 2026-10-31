# Builds bladeRF.dll for the host architecture with MSVC and copies it, with the
# libusb DLL, to dist\native\windows-<arch>.
#
# Usage: native\build-windows.ps1 -Arch x64|ARM64 [-Ref <bladeRF-git-ref>]
#
# Windows uses the prebuilt libusb binaries published by the libusb project. The
# C runtime is linked statically, so that the DLL needs nothing installed.
param(
    [Parameter(Mandatory = $true)][ValidateSet('x64', 'ARM64')][string]$Arch,
    [string]$Ref = ''
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

$Root = Split-Path -Parent $PSScriptRoot
$Versions = @{}
Get-Content "$Root\native\versions.env" | Where-Object { $_ -match '^[A-Z0-9_]+=' } | ForEach-Object {
    $Key, $Value = $_ -split '=', 2
    $Versions[$Key] = $Value
}
if (-not $Ref) { $Ref = $Versions.BLADERF_REF }
$LibusbVersion = $Versions.LIBUSB_VERSION

if ($Arch -eq 'x64') {
    $Platform = 'windows-x86_64'
    $UsbDir = 'VS2022\MS64\dll'
} else {
    $Platform = 'windows-aarch64'
    $UsbDir = 'VS2025\ARM64\dll'
}
$Out = "$Root\dist\native\$Platform"
$Licenses = "$Root\dist\native\licenses"
$Work = "$Root\build\native-work"
Remove-Item -Recurse -Force $Work, $Out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $Work, $Out, "$Licenses\bladeRF" | Out-Null
Set-Location $Work

Write-Host "=== libusb $LibusbVersion ==="
$Archive = "libusb-$LibusbVersion.7z"
Invoke-WebRequest "https://github.com/libusb/libusb/releases/download/v$LibusbVersion/$Archive" -OutFile $Archive
$Hash = (Get-FileHash $Archive -Algorithm SHA256).Hash
if ($Hash -ine $Versions.LIBUSB_WINDOWS_7Z_SHA256) {
    throw "libusb archive checksum mismatch: $Hash"
}
7z x $Archive -ousb | Out-Null

Write-Host "=== libbladeRF $Ref ==="
# Upstream patches its copy of the AD9361 driver during the build, and the patches
# do not apply to files that git has converted to CRLF. The setting also reaches
# the submodule. The AD9361 driver is the only submodule that libbladeRF needs.
git -c core.autocrlf=false -c advice.detachedHead=false clone --quiet --depth 1 --branch $Ref $Versions.BLADERF_REPOSITORY bladeRF
git -c core.autocrlf=false -C bladeRF submodule --quiet update --init --depth 1 thirdparty/analogdevicesinc/no-OS
$Commit = (git -C bladeRF rev-parse HEAD).Trim()
# Only the library is built. Skipping the utilities avoids their submodule.
Set-Content -Path 'bladeRF\host\utilities\CMakeLists.txt' -Value ''

$CMakeLists = Get-Content 'bladeRF\host\libraries\libbladeRF\CMakeLists.txt'
$UpstreamVersion = (@('MAJOR', 'MINOR', 'PATCH') | ForEach-Object {
    $Part = $_
    $Line = $CMakeLists | Where-Object { $_ -match "^set\(VERSION_INFO_$Part +([0-9]+)\)" } | Select-Object -First 1
    [void]($Line -match '([0-9]+)\)')
    $Matches[1]
}) -join '.'
if ($Ref -eq $Versions.BLADERF_REF -and $UpstreamVersion -ne $Versions.BLADERF_VERSION) {
    throw "$Ref contains libbladeRF $UpstreamVersion, not BLADERF_VERSION $($Versions.BLADERF_VERSION)"
}

# The libusb hints replace upstream's search, which has no paths for ARM64. The
# version check is skipped because it runs a program, and the value is given
# directly so the features that depend on it stay enabled.
$Header = (Get-ChildItem -Recurse -Path usb\include -Filter libusb.h | Select-Object -First 1).FullName
# Upstream's build runs the first patch on PATH. On GitHub runners that can be
# the one in Strawberry Perl, which is too old and crashes on upstream's patches.
# git's exec path is <git>\<platform>\libexec\git-core on every architecture.
$Patch = Join-Path (git --exec-path).Trim() '..\..\..\usr\bin\patch.exe'
if (-not (Test-Path $Patch)) { throw "Git for Windows patch not found: $Patch" }
$CMakeArgs = @(
    '-S', 'bladeRF\host', '-B', 'build', '-A', $Arch,
    '-DCMAKE_DISABLE_FIND_PACKAGE_PkgConfig=ON',
    "-DPatch_EXECUTABLE=$((Resolve-Path $Patch).Path.Replace('\', '/'))",
    '-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded',
    "-DLIBUSB_HEADER_FILE=$($Header.Replace('\', '/'))",
    "-Dusb_LIBRARY=$("$Work\usb\$UsbDir\libusb-1.0.lib".Replace('\', '/'))",
    '-DLIBUSB_SKIP_VERSION_CHECK=ON',
    "-DLIBUSB_VERSION=$LibusbVersion",
    '-DENABLE_BACKEND_CYAPI=OFF',
    '-DTREAT_WARNINGS_AS_ERRORS=OFF',
    '-DTEST_LIBBLADERF=OFF'
)
if ($Ref -eq $Versions.BLADERF_REF) {
    # Drops the -git suffix from the version the library reports.
    $CMakeArgs += '-DVERSION_INFO_EXTRA='
}
cmake @CMakeArgs
cmake --build build --config Release --parallel --target libbladerf_shared

Write-Host "=== packaging $Platform ==="
$Dll = Get-ChildItem -Recurse -Path build -Filter bladeRF.dll | Select-Object -First 1
if (-not $Dll) { throw 'bladeRF.dll was not produced' }
Copy-Item $Dll.FullName "$Out\bladeRF.dll"
Copy-Item "$Work\usb\$UsbDir\libusb-1.0.dll" "$Out\libusb-1.0.dll"
Copy-Item 'bladeRF\COPYING' "$Licenses\bladeRF\COPYING"
Copy-Item 'bladeRF\legal\licenses\*' "$Licenses\bladeRF\"
# no-OS has no license file: its terms are at the top of each source file.
$License = foreach ($Line in Get-Content 'bladeRF\thirdparty\analogdevicesinc\no-OS\ad9361\sw\ad9361.c') {
    $Line
    if ($Line -match '\*{8}/$') { break }
}
$License | Set-Content -Encoding ascii "$Licenses\ad9361-LICENSE.txt"
@(
    "bladerf.ref=$Ref",
    "bladerf.commit=$Commit",
    "bladerf.version=$UpstreamVersion",
    "libusb.version=$LibusbVersion"
) | Set-Content -Encoding ascii "$Out\build-info.properties"
Get-ChildItem $Out
