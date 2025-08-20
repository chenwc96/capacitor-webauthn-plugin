#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(WebAuthn, "WebAuthn",
           CAP_PLUGIN_METHOD(initialize, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(isEligible, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getBiometricsState, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(createCredential, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getAssertion, CAPPluginReturnPromise);
)
