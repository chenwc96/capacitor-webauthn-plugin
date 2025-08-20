# capacitor-webauthn-plugin

Capacitor plugin for using webauthn protocol in iOS and Android

# How to install

For Capacitor v2

To download and build the plugin, go to the parent folder of your ionic app and perform the steps below
```bash
git clone --branch v0.0.18 https://github.com/manulife-gwam/capacitor-webauthn-plugin
cd capacitor-webauthn-plugin
npm install
npm run build
```

To install the plugin in your ionic app
```bash
npm install ../capacitor-webauthn-plugin --save
```

# Usage

For Capacitor v2

In a component where you want to user this plugin add to or modify imports:

```javascript
import 'capacitor-webauthn-plugin';
import { Plugins } from '@capacitor/core';
const { WebAuthn } = Plugins;
```
First line is needed because of web part of the plugin (current behavior of Capacitor, this may change in future releases).

# Android

In Android, you have to register plugins manually in MainActivity class of your app.

https://capacitor.ionicframework.com/docs/plugins/android/#export-to-capacitor

```java
import com.manulife.mim.plugins.webauthn.WebAuthn;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
        // Initializes the Bridge
        this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
          // Additional plugins you've installed go here
          // Ex: add(TotallyAwesomePlugin.class);
          add(WebAuthn.class);
        }});
    }
}
```

You will also need to create a digital asset link (https://developers.google.com/digital-asset-links/v1/getting-started) and put the link into AndroidManifest.xml
```xml
<application>
    ...
    <meta-data android:name="asset_statements"
               android:resource="https://domain.name/.well-known/assetlinks.json" />
    ...
</application>
```

# Methods
* initialize(options: { authServerBaseURL: string }): Promise<void>
    * This method should be called during initialization. 
        * authServerBaseURL - base server URL of the authentication server
* isEligible(): Promise<{ isEligible: boolean , message: string }>
    * To check whether the device is eligible for WebAuthn
* getBiometricsState(options: { previousState: string }): Promise<{ state: string }>
    * The input parameter is the state that returned from this method. You may pass null or empty string if you don't have previous state 
    * Returns current biometrics state. Can be used to check if there is any changes in biometrics state
* createCredential({ payload: {} }): Promise<{ credential: {} }>
    * To create credential by biometrics
* getAssertion({ payload: {} }): Promise<{ assertion: {} }>
    * To assert credential by biometrics
    
# Example
```javascript
await WebAuthn.initialize({ authServerBaseURL: 'https://example.com' });
```

```javascript
const { isEligible } = await WebAuthn.isEligible();
```

```javascript
const { state } = await WebAuthn.getBiometricsState({ previousState: 'xxx' });
```

```javascript
const payload = {
  payload: {
    "attestation": "direct",
    "authenticatorSelection": {
      "authenticatorAttachment": "platform",
      "userVerification": "required"
    },
    "challenge": "...",
    "excludeCredentials": [],
    "pubKeyCredParams": [
      ...
    ],
    "rp": {
      "id": "...",
      "name": "..."
    },
    "timeout": 1800000,
    "user": {
      "displayName": "...",
      "id": "...",
      "name": "..."
    }
  }
}
const credential = await WebAuthn.createCredential(payload);
// credential = {
//   "type": "public-key",
//   "id": "...",
//   "rawId": "...",
//   "response": {
//     "attestationObject": "xxx",
//     "clientDataJSON": "xxx"
//   }
// }
```

```javascript
const payload = {
  "allowCredentials": [
    ...
  ],
  "challenge": "xxx",
  "rpId": "xxx",
  "timeout": 1800000,
  "userVerification": "required"
} 
const assertion = await WebAuthn.getAssertion(payload);
// assertion = {
//   "type": "public-key",
//   "id": "xxx",
//   "rawId": "xxx",
//   "response": {
//     "authenticatorData": "xxx", 
//     "clientDataJSON": "xxx",
//     "signature": "xxx",
//     "userHandle": "xxx" 
//   }
// }
```

#Error table

Error code | Error Description | iOS | Android 
------------ | ------------- | ------------- | -------------
1000 | Unknown Error | X | X
1001 | Authenticated Failed | X | 
1002 | The user tapped the cancel button in the authentication dialog. | X |
1003 | The user tapped the fallback button in the authentication dialog, but no fallback is available for the authentication policy. | X |
1004 | The system canceled authentication. | X |
1005 | A passcode isn’t set on the device. | X |
1006 | Touch ID is not available on the device. | X | X
1007 | The user has no enrolled Touch ID fingers. | X | X
1008 | Touch ID is locked because there were too many failed attempts. | X |
1009 | The app canceled authentication. | X |
1010 | The context was previously invalidated. | X |
1011 | Displaying the required authentication user interface is forbidden. | X |
1012 | Biometry is locked because there were too many failed attempts. | X |
1013 | Biometry is not available on the device. | X |
1014 | The user has no enrolled biometric identities. | X | X
2001 | Bad Data | X | X
2002 | Bad Operation | X |
2003 | Invalid State | X |
2004 | Constraint | X |
2005 | Cancelled | X | X
2006 | Timeout | X |
2007 | Not Allowed | X | X
2008 | Unsupported | X |
