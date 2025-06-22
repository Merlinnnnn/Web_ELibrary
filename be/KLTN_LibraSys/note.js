export async function encryptClientKey(serverPublicKeyBase64) {
    try {
        // 1. Import publicKey từ server (base64 → ArrayBuffer)
        const publicKeyDer = Uint8Array.from(atob(serverPublicKeyBase64), c => c.charCodeAt(0));
        const publicKey = await crypto.subtle.importKey(
            'spki',
            publicKeyDer.buffer,
            { name: 'RSA-OAEP', hash: 'SHA-256' },
            true,
            ['encrypt']
        );

        // 2. Tạo clientKey AES-256 (ngẫu nhiên)
        const clientKey = await crypto.subtle.generateKey(
            { name: 'AES-CBC', length: 256 },
            true,
            ['encrypt', 'decrypt']
        );

        // 3. Export clientKey dưới dạng raw để mã hóa
        const clientKeyRaw = await crypto.subtle.exportKey('raw', clientKey);

        // 4. Mã hóa clientKey bằng publicKey của server (RSA-OAEP)
        const encryptedKeyBuffer = await crypto.subtle.encrypt(
            { name: 'RSA-OAEP' },
            publicKey,
            clientKeyRaw
        );
        const clientKeyBase64 = btoa(String.fromCharCode(...new Uint8Array(clientKeyRaw)));
        return {


            encryptedClientKeyBase64: btoa(String.fromCharCode(...new Uint8Array(encryptedKeyBuffer))),
            clientKey: clientKey, // Để giữ trong memory sử dụng tiếp
            clientKeyBase64
        };
    } catch (error) {
        console.error("Lỗi khi tạo và mã hóa client key:", error);
        throw error;
    }
}


/**
 * Giải mã content key đã được mã hóa bằng AES-GCM
 * @param {string} encryptedContentKeyBase64 - Dữ liệu mã hóa (IV + ciphertext) ở dạng Base64
 * @param {string} clientKeyBase64 - Client key ở dạng Base64
 * @returns {Promise<Uint8Array>} - Mảng byte của content key đã giải mã
 */
export async function decryptContentKey(encryptedContentKeyBase64, clientKeyBase64) {
    try {
        // 1. Chuyển base64 sang Uint8Array
        function base64ToUint8Array(base64) {
            const binary = atob(base64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            return bytes;
        }

        const encryptedContentKey = base64ToUint8Array(encryptedContentKeyBase64);
        const clientKey = base64ToUint8Array(clientKeyBase64);

        console.log("JS - Encrypted content key length:", encryptedContentKey.length);
        console.log("JS - Client key length:", clientKey.length);

        // 2. Tách IV (12 byte đầu) và ciphertext
        const iv = encryptedContentKey.slice(0, 12);
        const ciphertext = encryptedContentKey.slice(12);

        console.log("JS - IV length:", iv.length);
        console.log("JS - Ciphertext length:", ciphertext.length);

        // 3. Import khóa AES
        const cryptoKey = await crypto.subtle.importKey(
            "raw",
            clientKey,
            { name: "AES-GCM" },
            false,
            ["decrypt"]
        );

        // 4. Giải mã
        const decryptedBuffer = await crypto.subtle.decrypt(
            {
                name: "AES-GCM",
                iv: iv,
                tagLength: 128
            },
            cryptoKey,
            ciphertext
        );

        const decryptedContentKey = new Uint8Array(decryptedBuffer);
        console.log("JS - Decrypted content key length:", decryptedContentKey.length);

        // Nếu cần trả về Base64 để debug
        const decryptedBase64 = btoa(String.fromCharCode(...decryptedContentKey));
        console.log("JS - Decrypted content key (Base64):", decryptedBase64);

        return decryptedContentKey;
    } catch (e) {
        console.error("❌ Lỗi giải mã content key:", e.name, e.message);
        throw e;
    }
}
