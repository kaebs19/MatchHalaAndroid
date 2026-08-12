# AdMob support ticket — ad requests rejected with HTTP 403

Publisher ID: pub-8219247197168750
App: Hala Chat — Meet & Chat (com.chathala.hala)
App ID: ca-app-pub-8219247197168750~3358932717
Ad units: 5140663498 (banner), 9723886272 (interstitial), 7097722938 (native)

All ad requests from real devices are rejected with HTTP 403, so the app has
never served a single impression.

What I verified before contacting you:

- App status is "Ready" and the app is verified (Google Play, live).
- Policy centre shows "No current issues".
- The account has been verified for years; address verification is completed.
- app-ads.txt is published at https://chathala.com/app-ads.txt and returns 200
  with: google.com, pub-8219247197168750, DIRECT, f08c47fec0942fa0
- The AdMob App ID in AndroidManifest matches the console exactly.
- Tested on a real device (Xiaomi 25057RN09G, Android 16, Saudi Arabia) on two
  different networks — Wi-Fi and STC mobile data — with identical results.
- The device has no VPN, no proxy, no private DNS, and a correct system clock.
- I registered the device with RequestConfiguration.setTestDeviceIds(). The SDK
  confirms "This request is sent from a test device", yet the request is STILL
  rejected with HTTP 403. Even Google test ads are refused.

Device logcat (Google Mobile Ads SDK afma-sdk-a-v261710999.244410000.1):

    I Ads     : This request is sent from a test device.
    W Ads     : Not retrying to fetch app settings
    W Ads     : Received error HTTP response code: 403
    I Ads     : Ad failed to load : 3

This affects banner, interstitial and native units alike. Note that even the
app settings fetch — which happens before any ad request — is rejected. A
genuine no-fill returns HTTP 200 with an empty body, not 403, and test ads are
never subject to fill. This points to an account or app level rejection on the
ad server rather than a policy, fill, network or integration issue.

Could you please check why ad serving is refused for this publisher/app, and
what I need to do to restore it?
