import { WebPlugin } from '@capacitor/core';
import { WebAuthnPlugin } from './definitions';
export declare class WebAuthnWeb extends WebPlugin implements WebAuthnPlugin {
    constructor();
    initialize(options: {
        authServerBaseURL: string;
    }): Promise<void>;
    isEligible(): Promise<{
        isEligible: boolean;
        message: string;
    }>;
    getBiometricsState(options: {
        previousState: string;
    }): Promise<{
        state: string;
    }>;
    createCredential(options: {
        payload: any;
    }): Promise<{
        credential: any;
    }>;
    getAssertion(options: {
        payload: any;
    }): Promise<{
        assertion: any;
    }>;
}
declare const WebAuthn: WebAuthnWeb;
export { WebAuthn };
