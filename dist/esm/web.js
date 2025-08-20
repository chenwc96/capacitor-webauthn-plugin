var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { WebPlugin } from '@capacitor/core';
export class WebAuthnWeb extends WebPlugin {
    constructor() {
        super({
            name: 'WebAuthn',
            platforms: ['web'],
        });
    }
    initialize(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.log(`[web][initialize] not implemented ${options}`);
        });
    }
    isEligible() {
        return __awaiter(this, void 0, void 0, function* () {
            console.log('[web][isEligible] not implemented');
            return { isEligible: false, message: "not implemented" };
        });
    }
    getBiometricsState(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.log(`[web][getBiometricsState] not implemented ${options.previousState}`);
            return { state: "" };
        });
    }
    createCredential(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.log(`[web][createCredential] not implemented ${options.payload}`);
            return { credential: { error: 'not implemented' } };
        });
    }
    getAssertion(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.log(`[web][getAssertion] not implemented ${options.payload}`);
            return { assertion: { error: 'not implemented' } };
        });
    }
}
const WebAuthn = new WebAuthnWeb();
export { WebAuthn };
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(WebAuthn);
//# sourceMappingURL=web.js.map