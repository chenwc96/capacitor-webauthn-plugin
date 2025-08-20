import { WebPlugin } from '@capacitor/core';
import { WebAuthnPlugin } from './definitions';

export class WebAuthnWeb extends WebPlugin implements WebAuthnPlugin {
  constructor() {
    super({
      name: 'WebAuthn',
      platforms: ['web'],
    });
  }

  async initialize(options: { authServerBaseURL: string }) {
    console.log(`[web][initialize] not implemented ${options}`)
  }

  async isEligible(): Promise<{ isEligible: boolean, message: string }> {
    console.log('[web][isEligible] not implemented')
    return { isEligible: false, message: "not implemented" }
  }

  async getBiometricsState(options: { previousState: string }): Promise<{ state: string }> {
    console.log(`[web][getBiometricsState] not implemented ${options.previousState}`)
    return { state: "" }
  }

  async createCredential(options: { payload: any }): Promise<{ credential: any }> {
    console.log(`[web][createCredential] not implemented ${options.payload}`)
    return { credential: { error: 'not implemented' }}
  }

  async getAssertion(options: { payload: any }): Promise<{ assertion: any }> {
    console.log(`[web][getAssertion] not implemented ${options.payload}`)
    return { assertion: { error: 'not implemented' }}
  }
}

const WebAuthn = new WebAuthnWeb();

export { WebAuthn };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(WebAuthn);
