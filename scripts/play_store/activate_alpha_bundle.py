
import os
import sys
import re
from google.oauth2 import service_account
from googleapiclient.discovery import build

def get_package_name():
    gradle_path = os.path.join(os.path.dirname(__file__), "../../app/build.gradle.kts")
    if not os.path.exists(gradle_path):
        return "at.priv.graf.zazentimer"
    with open(gradle_path, "r") as f:
        content = f.read()
        match = re.search(r'(namespace|applicationId)\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(2)
    return "at.priv.graf.zazentimer"

def activate_bundle(version_code, track_name='alpha'):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    key_path = os.path.join(script_dir, "../../google/play-api-key.json")
    package_name = get_package_name()

    if not os.path.exists(key_path):
        print(f"Error: Key not found at {key_path}")
        sys.exit(1)

    scopes = ['https://www.googleapis.com/auth/androidpublisher']
    credentials = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    service = build('androidpublisher', 'v3', credentials=credentials)

    # 1. Create a new edit
    edit_request = service.edits().insert(packageName=package_name, body={})
    edit = edit_request.execute()
    edit_id = edit['id']

    # 2. Get the track
    track = service.edits().tracks().get(
        packageName=package_name,
        editId=edit_id,
        track=track_name
    ).execute()

    if 'releases' not in track or not track['releases']:
        print(f"No releases found in track {track_name}. Creating a new one.")
        track['releases'] = [{
            'name': '3.0.3',
            'status': 'completed',
            'versionCodes': [str(version_code)]
        }]
    else:
        # Update the latest release in the track
        latest_release = track['releases'][0]
        latest_release['versionCodes'] = [str(version_code)]
        latest_release['status'] = 'completed'
        print(f"Updating release {latest_release.get('name')} in track {track_name} with version {version_code}")

    # 3. Update the track in the edit
    service.edits().tracks().update(
        packageName=package_name,
        editId=edit_id,
        track=track_name,
        body=track
    ).execute()

    # 4. Commit the changes
    service.edits().commit(packageName=package_name, editId=edit_id).execute()
    print(f"Successfully activated version {version_code} in track {track_name} for {package_name}.")

if __name__ == "__main__":
    # The version code 3000300 was seen in the screenshot as deactivated
    VERSION_CODE = 3000300
    TRACK = 'alpha'
    
    try:
        activate_bundle(VERSION_CODE, TRACK)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)
