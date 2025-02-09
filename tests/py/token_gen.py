import random
import re

# Define the allowed characters based on the regex pattern
allowed_chars = '#;&%$:?=+-*abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'

# Generate a 32-byte token
token = ''.join(random.choice(allowed_chars) for _ in range(32))

# Ensure the token matches the regex pattern
if re.fullmatch(r'[#;&%$:?=+\-*a-zA-Z0-9]{32}', token):
    print("Generated Token:", token)
else:
    print("Token generation failed. Please try again.")