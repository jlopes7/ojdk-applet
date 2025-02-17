import sys
import json
import base64
import struct
from Cryptodome.Cipher import DES3
from Cryptodome.Util.Padding import pad

def load_json_file(file_path):
    """Read a JSON file and return its content as a string."""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            return file.read()
    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file: {e}", file=sys.stderr)
        sys.exit(1)

def des3_encrypt(key_b64, plaintext):
    """Encrypts the plaintext using DES3 and returns the Base64-encoded ciphertext."""
    try:
        # Decode the Base64 key
        key = base64.b64decode(key_b64)

        # Ensure key length is exactly 24 bytes
        if len(key) != 24:
            print("Error: DES3 key must be 24 bytes long.", file=sys.stderr)
            sys.exit(1)

        # Create a DES3 cipher in ECB mode
        cipher = DES3.new(key, DES3.MODE_ECB)

        # Ensure plaintext is a multiple of 8 bytes (DES block size)
        #if len(plaintext) % 8 != 0:
        #    print(f"Error: Plaintext must be a multiple of 8 bytes for DES3 encryption without padding. Size: {len(plaintext)} bytes", file=sys.stderr)
        #    sys.exit(1)

        # Pad plaintext to be a multiple of 8 bytes (DES block size)
        padded_plaintext = pad(plaintext.encode(), DES3.block_size)

        # Encrypt and encode to Base64
        encrypted_data = cipher.encrypt(padded_plaintext)
        # Debug: Print encrypted bytes as hex (similar to C output)
        print("Encrypted Ciphertext (Hex):")
        print(" ".join(f"{byte:02X}" for byte in encrypted_data))  # Equivalent to "%02X " in C
        print()

        encrypted_b64 = base64.b64encode(encrypted_data).decode()

        return encrypted_b64
    except Exception as e:
        print(f"Encryption error: {e}", file=sys.stderr)
        sys.exit(1)

def save_native_message(output_file, json_payload):
    """Writes a JSON message to a file using Chrome's Native Messaging protocol."""
    try:
        # Convert JSON object to a compact string (without extra whitespace/newlines)
        json_bytes = json.dumps(json_payload, separators=(',', ':')).encode('utf-8')

        # Get message length (uint32_t, little-endian)
        msg_length = struct.pack('<I', len(json_bytes))

        # Write to file
        with open(output_file, 'wb') as file:
            file.write(msg_length + json_bytes)

        print(f"Payload size: {len(json_bytes)} bytes")

    except Exception as e:
        print(f"Error writing output file: {e}", file=sys.stderr)
        sys.exit(1)

def main():
    if len(sys.argv) != 4:
        print("Usage: python script.py <input_json_file> <base64_des3_key> <output_file>", file=sys.stderr)
        sys.exit(1)

    json_file = sys.argv[1]
    key_b64 = sys.argv[2]
    output_file = sys.argv[3]

    # Read the JSON file as a string
    plaintext = load_json_file(json_file)

    # Encrypt the content using DES3
    encrypted_payload = des3_encrypt(key_b64, plaintext)

    # Create the output JSON
    output_json = {
        "payload": encrypted_payload,
        "msgsize": len(plaintext)
    }

    # Save the JSON message in Chrome Native Messaging format
    save_native_message(output_file, output_json)

if __name__ == "__main__":
    main()
