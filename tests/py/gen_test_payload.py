import json

message = {
    "op": "load_applet",
    "className": "JavaVersionDisplayApplet.class",
    "archiveUrl": "",
    "codebase": "",
    "appletName": "",
    "baseUrl": "https://javatester.org/",
    "width": "440",
    "height": "60",
    "posx": 1530.5,
    "posy": 299.9375,
    "parameters": "",
    "cookies": ""
}

message_str = json.dumps(message)
message_bytes = len(message_str).to_bytes(4, byteorder="little") + message_str.encode("utf-8")

print(f"Genearing payload file ../oplauncher/test_input.bin ...")
# Save to a binary file
with open("test_input.bin", "wb") as f:
    f.write(message_bytes)

print("Generated test_input.bin with the correct format.")
