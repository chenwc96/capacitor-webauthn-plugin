declare module '@capacitor/core' {
  interface PluginRegistry {
    WebAuthn: WebAuthnPlugin;
  }
}

export interface WebAuthnPlugin {
  initialize(options: { authServerBaseURL: string }): Promise<void>;
  isEligible(): Promise<{ isEligible: boolean , message: string }>;
  getBiometricsState(options: { previousState: string }): Promise<{ state: string }>;
  createCredential(options: { payload: any }): Promise<{ credential: any }>;
  getAssertion(options: { payload: any }): Promise<{ assertion: any }>;
}
