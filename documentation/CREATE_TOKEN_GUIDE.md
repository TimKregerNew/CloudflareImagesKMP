# Create Cloudflare API Token - Step by Step

## ⚠️ Your current token is NOT working

The error "Unable to authenticate request" means either:
- Token doesn't have correct permissions
- Token is invalid/expired
- Account ID is wrong

## 🔧 Create a Working Token

### Step 1: Go to API Tokens Page
Open: https://dash.cloudflare.com/profile/api-tokens

### Step 2: Click "Create Token"

### Step 3: Use "Create Custom Token"
- **Do NOT use** "Edit Cloudflare Images" template (it might be restricted)
- Click **"Get started"** under "Create Custom Token"

### Step 4: Configure the Token

**Token name:**
```
KMP Mobile App - Images Upload
```

**Permissions:** (Click "+ Add more")
```
Account → Cloudflare Images → Edit
```

**Account Resources:**
```
Include → All accounts
```
OR select your specific account

**TTL (Time to Live):**
```
Optional: Set expiration date or leave blank for no expiry
```

### Step 5: Continue and Create

Click **"Continue to summary"** → **"Create Token"**

### Step 6: **COPY THE TOKEN IMMEDIATELY**

⚠️ You will only see it once!

It should look like: `xxxx-yyyyyyyyyyyyyyyyyyyyyyyy`

### Step 7: Test the Token BEFORE Using

Open Terminal and run:

```bash
# Replace YOUR_ACCOUNT_ID and YOUR_NEW_TOKEN with your actual values
curl -X GET \
  "https://api.cloudflare.com/client/v4/accounts/YOUR_ACCOUNT_ID/images/v1/stats" \
  -H "Authorization: Bearer YOUR_NEW_TOKEN"
```

**Expected response (success):**
```json
{
  "success": true,
  "result": {
    "count": {
      "current": 0,
      "allowed": 100000
    }
  }
}
```

**If you see an error:**
```json
{
  "success": false,
  "errors": [{"code": 10001, "message": "Unable to authenticate request"}]
}
```
Then the token is still wrong - go back to Step 1

### Step 8: Update Your Script

Once the curl test works, set your environment variables:

```bash
export CLOUDFLARE_ACCOUNT_ID="your-account-id"
export CLOUDFLARE_API_TOKEN="YOUR_WORKING_TOKEN_HERE"
```

### Step 9: Run Integration Tests

```bash
./testScripts/run-integration-tests.sh
```

## 🔍 Verify It Worked

You should see output like:
```
🧪 Testing image upload to Cloudflare...
✅ Upload successful!
   Image ID: abc-123
   Filename: integration-test.png
...
Cleaned up test image: abc-123
```

## ❓ Still Not Working?

### Check Account ID
Go to: https://dash.cloudflare.com/ → Click on any site → Look in URL bar
```
https://dash.cloudflare.com/YOUR_ACCOUNT_ID_HERE/...
```

Make sure it matches your account ID (e.g., `YOUR_ACCOUNT_ID`)

### Check Images is Enabled
Go to: https://dash.cloudflare.com/YOUR_ACCOUNT_ID/images

If you see "Enable Cloudflare Images" button, you need to enable it first!

### Token Permissions Checklist
Your token MUST have:
- ✅ Permission: Cloudflare Images → **Edit** (not Read)
- ✅ Resource: Include → Your account
- ✅ Status: Active (not expired)

## 📸 Want to See Images in Dashboard?

The integration tests **clean up after themselves**. To upload a test image that stays:

```bash
# After your token works, run this:
ACCOUNT_ID="YOUR_ACCOUNT_ID"
TOKEN="your-working-token"

curl -X POST \
  "https://api.cloudflare.com/client/v4/accounts/$ACCOUNT_ID/images/v1" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/any/image.jpg" \
  -F "id=manual-test-image"
```

Then check dashboard: https://dash.cloudflare.com/YOUR_ACCOUNT_ID/images

You should see "manual-test-image" there!

