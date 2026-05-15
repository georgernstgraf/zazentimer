import json
import os
import glob
import re

REPORT_DIR = "docs/ai/translation_reports"
RES_DIR = "app/src/main/res"

def escape_xml(text):
    """Sicheres Escaping von XML-Sonderzeichen."""
    # We must not escape existing %1$d or \n
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace('"', '\\"')

def main():
    report_files = glob.glob(os.path.join(REPORT_DIR, "*_report.json"))
    
    total_applied = 0
    errors = []
    
    print(f"Starte Verarbeitung von {len(report_files)} Report-Dateien...\n")
    
    for report_file in report_files:
        with open(report_file, 'r', encoding='utf-8') as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError as e:
                errors.append(f"[JSON ERROR] Konnte {report_file} nicht parsen: {e}")
                continue
                
        locale = data.get('locale')
        findings = data.get('findings', [])
        
        if not findings:
            continue # Nichts zu tun fuer diese Sprache
            
        # Baue Pfad zusammen - Android verwendet 'values-{locale}'
        xml_path = os.path.join(RES_DIR, f"values-{locale}", "strings.xml")
        
        # Sonderbehandlung fuer base sr-Latn (Android erwartet b+sr+Latn)
        if locale == "b+sr+Latn":
            xml_path = os.path.join(RES_DIR, "values-b+sr+Latn", "strings.xml")
        
        if not os.path.exists(xml_path):
            errors.append(f"[FILE ERROR] Zieldatei fehlt fuer Locale '{locale}': {xml_path}")
            continue
            
        with open(xml_path, 'r', encoding='utf-8') as f:
            xml_content = f.read()
            
        changes_made = False
        for finding in findings:
            key = finding.get('string_key')
            new_text = finding.get('suggested_translation')
            
            if not key or not new_text:
                errors.append(f"[DATA ERROR] Fehlender Key oder Text in {report_file}")
                continue
                
            safe_text = escape_xml(new_text)
            
            # RegEx sucht nach <string name="key">...irgendwas...</string>
            pattern = r'(<string[^>]*name="' + re.escape(key) + r'"[^>]*>)(.*?)(</string>)'
            
            if re.search(pattern, xml_content):
                # Wir muessen re.escape fuer den Replacement-String nutzen, da Backslashes in Regex-Replacements sonst Probleme machen
                # Aber wir verwenden ein Lambda, um Backslash-Escaping-Probleme komplett zu umgehen
                xml_content = re.sub(pattern, lambda m: m.group(1) + safe_text + m.group(3), xml_content)
                changes_made = True
                total_applied += 1
            else:
                errors.append(f"[MISSING KEY ERROR] Key '{key}' nicht gefunden in {locale} ({xml_path})")
                
        if changes_made:
            with open(xml_path, 'w', encoding='utf-8') as f:
                f.write(xml_content)
                
    # --- Zusammenfassung ---
    print("=" * 40)
    print("ZUSAMMENFASSUNG DER UBERSETZUNGS-KORREKTUREN")
    print("=" * 40)
    print(f"Erfolgreich angewendete Korrekturen: {total_applied}")
    print(f"Fehler / Uebersprungene Items: {len(errors)}")
    
    if errors:
        print("\n--- FEHLERPROTOKOLL ---")
        for err in errors:
            print(err)

if __name__ == "__main__":
    main()