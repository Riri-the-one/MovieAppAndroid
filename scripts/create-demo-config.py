"""Generate a nonfunctional Firebase client config for compilation only."""
import json
from pathlib import Path

target = Path(__file__).resolve().parents[1] / "app/google-services.json"
if target.exists():
    raise SystemExit("Refusing to overwrite an existing Firebase configuration")
target.write_text(json.dumps({
    "project_info": {"project_number": "123456789012", "project_id": "demo-movies-portfolio", "storage_bucket": "demo-movies-portfolio.appspot.com"},
    "client": [{"client_info": {"mobilesdk_app_id": "1:123456789012:android:0123456789abcdef", "android_client_info": {"package_name": "com.example.movies_app"}}, "oauth_client": [], "api_key": [{"current_key": "demo-not-a-real-api-key"}], "services": {"appinvite_service": {"other_platform_oauth_client": []}}}],
    "configuration_version": "1"
}, indent=2), encoding="utf-8")
