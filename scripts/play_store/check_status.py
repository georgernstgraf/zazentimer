
import os
import sys
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build

def get_service(key_path):
    scopes = ['https://www.googleapis.com/auth/androidpublisher']
    credentials = service_account.Credentials.from_service_account_file(key_path, scopes=scopes)
    return build('androidpublisher', 'v3', credentials=credentials)

def check_status(package_name, key_path):
    service = get_service(key_path)
    
    # 2. Check Tracks and Releases
    edit_request = service.edits().insert(packageName=package_name, body={})
    edit = edit_request.execute()
    edit_id = edit['id']
    
    tracks = service.edits().tracks().list(packageName=package_name, editId=edit_id).execute()
    
    print(f"\n--- Active Tracks for {package_name} ---")
    for track in tracks.get('tracks', []):
        name = track['track']
        print(f"\nTrack: {name.upper()}")
        for release in track.get('releases', []):
            ver = release.get('versionCodes', ['?'])
            status = release.get('status', 'unknown')
            release_name = release.get('name', 'no name')
            print(f"  [{status}] Version: {ver} ({release_name})")
            if 'releaseNotes' in release:
                for note in release['releaseNotes']:
                    lang = note.get('language', '??')
                    text = note.get('text', 'No notes')
                    print(f"    ({lang}): {text}")

if __name__ == "__main__":
    KEY_PATH = os.path.expanduser("~/.config/iron-country-322716-8ab0815de79f.json")
    PACKAGE_NAME = "at.priv.graf.zazentimer"
    
    if not os.path.exists(KEY_PATH):
        print(f"Error: Key not found at {KEY_PATH}")
        sys.exit(1)
        
    try:
        check_status(PACKAGE_NAME, KEY_PATH)
    except Exception as e:
        print(f"\nERROR: {e}")
