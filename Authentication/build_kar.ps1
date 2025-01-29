$BUNDLE_PATH_TO_BUILD = (Get-Location).Path

Write-Host "Building KAR file at $BUNDLE_PATH_TO_BUILD"

# Change to the directory
Set-Location -Path $BUNDLE_PATH_TO_BUILD

# Create a temporary directory
Write-Host "Creating temporary directory 'temp'"
New-Item -ItemType Directory -Path "temp" -Force

# Copy the KAR file from the BPM container
Write-Host "Copying the com.tibco.bpm.karaf.ace.kar from the BPM container to the temp directory"
docker cp bpm:/opt/tibco/tibco-karaf-1.0.0/deploy/com.tibco.bpm.karaf.bpm.kar .\temp\

Set-Location -Path "temp"

# Rename the KAR file to ZIP for extraction
Write-Host "Renaming com.tibco.bpm.karaf.bpm.kar to com.tibco.bpm.karaf.bpm.zip"
Rename-Item -Path "com.tibco.bpm.karaf.bpm.kar" -NewName "com.tibco.bpm.karaf.bpm.zip"

# Unzip the KAR file
Write-Host "Unzipping the com.tibco.bpm.karaf.ace.zip file to the temp directory"
Expand-Archive -Path "com.tibco.bpm.karaf.bpm.zip" -DestinationPath .

# Copy built bundles
Write-Host "Copying the built bundles from $BUNDLE_PATH_TO_BUILD to the temp directory"
Copy-Item -Recurse -Force ..\build-artifacts\repository\repository\com\tibco\bpm\karaf\ace\* .\repository\com\tibco\bpm\karaf\ace\

# Re-zip the KAR file
Write-Host "Zipping the com.tibco.bpm.karaf.ace.kar file which has newly built bundles"
Compress-Archive -Path * -DestinationPath "com.tibco.bpm.karaf.bpm.zip" -Force

# Rename the ZIP file back to KAR
Write-Host "Renaming com.tibco.bpm.karaf.bpm.zip back to com.tibco.bpm.karaf.bpm.kar"
Rename-Item -Path "com.tibco.bpm.karaf.bpm.zip" -NewName "com.tibco.bpm.karaf.bpm.kar"

# Copy the KAR file back to the BPM container
Write-Host "Copying the newly built com.tibco.bpm.karaf.ace.kar file to the BPM container"
docker cp com.tibco.bpm.karaf.bpm.kar bpm:/opt/tibco/tibco-karaf-1.0.0/deploy/

Set-Location -Path ..

# Delete the temp directory
Write-Host "Deleting the temp directory"
#Remove-Item -Recurse -Force "temp"

# Restart the BPM container
Write-Host "Restarting the BPM container"
docker restart bpm