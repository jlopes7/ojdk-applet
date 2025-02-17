import os
import base64

# Generate a 24-byte DES3 key
key = os.urandom(24)

# Encode the key in Base64
encoded_key = base64.b64encode(key).decode('utf-8')

# Print the Base64-encoded key
print("Generated 24-byte DES3 Key (Base64 encoded):", encoded_key)
