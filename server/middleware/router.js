const { error } = require("console");
const db = require("../database/dbops");
const crypto = require("crypto");
const KoaRouter = require("koa-router");

const router = new KoaRouter();

let supermarketPublicKey = null;

/**
 * Converts a crypto public key to a Base64-encoded string in DER format.
 * @param {crypto.KeyObject} publicKey
 * @returns {string}
 */
function encodeKeyBase64(publicKey) {
    const publicKeyBytes = publicKey.export({ format: "der", type: "spki" });
    return publicKeyBytes.toString("base64");
}

/**
 * Loads a public key from a Base64-encoded string in DER format.
 * @param {string} base64Key
 * @returns {crypto.KeyObject}
 */
function loadKey(base64Key) {
    const buffer = Buffer.from(base64Key, "base64");
    return crypto.createPublicKey({
        key: buffer,
        format: "der",
        type: "spki",
    });
}

// initialize the database and set up routes
(async () => {
    try {
        await db.init();
        await startAutoCleanup();
        router
            .post("/key", actionMarketKey)
            .post("/users/add", actionRegistration)
            .post("/pay", actionPayment)
            .post("/challenge/vouchers", actionChallengeVouchers)
            .post("/challenge/transactions", actionChallengeTransaction)
            .post("/vouchers", actionGetVouchers)
            .post("/transactions", actionGetTransactions)
            .get("/groceries", actionGetGroceries);
    } catch (error) {
        console.error("Error initializing database:", error);
    }
})();

/**
 * Receives and stores the supermarket's RSA public key.
 * @param {*} ctx
 */
async function actionMarketKey(ctx) {
    console.log("\n---- START Action Market Key (router) ----");
    const { keyRSA } = ctx.request.body;
    supermarketPublicKey = loadKey(keyRSA);
    ctx.body = {};
    console.log("---- END Action Market Key (router) ----\n");
}

/**
 * Adds a new user to the database with EC and RSA keys.
 * @param {*} ctx
 */
async function actionRegistration(ctx) {
    console.log("\n---- START Action Registration (router) ----");

    // loading and verifying the keys
    const {
        keyEC,
        keyRSA,
        cardNumber,
        cardDate,
        selectedCardType,
    } = ctx.request.body;

    console.log("The user information is:");
    console.log("  - Card Number", cardNumber);
    console.log("  - Card Date", cardDate);
    console.log("  - Selected Card Type", selectedCardType);

    let ec_public_key = loadKey(keyEC);
    let rsa_public_key = loadKey(keyRSA);

    // perform registration
    const [uuid, result] = await db.actionRegistration(
        encodeKeyBase64(ec_public_key),
        encodeKeyBase64(rsa_public_key),
        cardNumber,
        cardDate,
        selectedCardType
    );

    // check if it was a bad request
    if (result === false) {
        ctx.status = 400;
        ctx.body = {};
    } else {
        ctx.body = {
            Uuid: uuid,
            key: encodeKeyBase64(supermarketPublicKey),
        };
    }

    console.log("---- END Action Registration (router) ----\n");
}

/**
 * Verifies the payment message and performs it if everything is right.
 * @param {*} ctx
 */
async function actionPayment(ctx) {
    console.log("\n---- START Action Payment (router) ----");

    try {
        const { message } = ctx.request.body;
        if (!message) throw new Error("The message is missing!");
        const [user, prods, disc, voucher, part, sign] = readPayment(message);

        // verifying signature
        const verifySign = await db.verifySignature(user, sign, part);
        if (verifySign === false) {
            throw new Error("Invalid signature!");
        }

        // calculating total value to pay
        let priceTotal = 0;
        let usedDiscount = 0;
        for (const prod of prods) {
            priceTotal += prod.price;
        }

        // apply discount if needed
        if (disc === true) {
            const discountRow = await db.getUserDiscount(user);
            if (discountRow === null) throw new Error("Invalid discount!");

            const discount = discountRow[1];

            // update price according with the discount
            if (priceTotal > discount) {
                priceTotal -= discount;
                usedDiscount = discount;
            } else {
                usedDiscount = priceTotal;
                priceTotal = 0;
            }
        }

        // calculating accumulated discount
        let accumulated = 0;
        if (voucher !== null) {
            const verifyVoucher = await db.verifyVoucher(user, voucher);
            if (verifyVoucher === false) throw new Error("Invalid voucher.");
            accumulated = priceTotal * 0.15;
        }

        // perform payment
        const result = await db.actionPayment(
            user,
            voucher,
            usedDiscount,
            priceTotal,
            accumulated
        );

        if (result === true) {
            ctx.body = { verified: result };
        } else {
            throw new Error("Invalid payment.");
        }
    } catch (error) {
        console.log(error);
        ctx.status = 400;
        ctx.body = {
            error: "Failed in payment transaction",
            details: error.message,
        };
    }

    console.log("---- END Action Payment (router) ----\n");
}

/**
 * Sends a nonce to the user for him to be able to retrieve his vouchers.
 * Saves it in the database to later retrieve.
 * @param {*} ctx
 */
async function actionChallengeVouchers(ctx) {
    console.log("\n---- START Action Challenge Vouchers (router) ----");

    try {
        // checking user
        let { user } = ctx.request.body;
        let [success, result] = await db.actionGetUser(user);

        if (success === true) {
            // perform adding of nonce
            [success, result] = await db.actionAddNonce(user, "VOUCHER");
            if (success === true) ctx.body = { nonce: result };
            else throw new Error(result);
        } else {
            throw new Error(result);
        }
    } catch (error) {
        console.log(error);
        ctx.status = 400;
        ctx.body = {};
    }

    console.log("---- END Action Challenge Vouchers (router) ----\n");
}

async function actionChallengeTransaction(ctx) {
    console.log("\n---- START Action Challenge Transaction (router) ----");

    try {
        let { user } = ctx.request.body;
        let [success, result] = await db.actionGetUser(user);

        if (success === true) {
            [success, result] = await db.actionAddNonce(user, "TRANSACTION");
            if (success === true) ctx.body = { nonce: result };
            else throw new Error(result);
        } else {
            throw new Error(result);
        }
    } catch (error) {
        console.log(error);
        ctx.status = 400;
        ctx.body = {};
    }

    console.log("\n---- END Action Challenge Vouchers (router) ----");
}

/**
 *
 * @param {*} ctx
 */
async function actionGetVouchers(ctx) {
    console.log("\n---- START Action Get Vouchers (router) ----");

    try {
        let { user, message } = ctx.request.body;
        const [success, result] = await db.actionGetVouchers(user, message);

        if (success === false) {
            ctx.body = { error: result };
        } else {
            const [success1, result1] = await db.getUserDiscount(user);
            if (success1 === false) {
                ctx.body = { error: result1 };
            } else {
                console.log(result1);
                ctx.body = {
                    vouchers: result,
                    discount: parseFloat(parseFloat(result1).toFixed(2)),
                };
            }
        }
    } catch (error) {
        console.log("Get Vouchers - ", error);
        ctx.status = 400;
        ctx.body = { error: error };
    }

    console.log("---- END Action Get Vouchers (router) ----\n");
}

async function actionGetGroceries(ctx) {
    console.log("\n---- START Action Get Groceries (router) ----");

    try {
        const [success, result] = await db.getGroceries();
        if (success === false) {
            ctx.body = { error: result };
        } else {
            ctx.body = {
                groceries: result,
            };
        }
    } catch (error) {
        ctx.status = 400;
        ctx.body = { error: error };
    }

    console.log("---- END Action Get Groceries (router) ----\n");
}

async function actionGetTransactions(ctx) {
    console.log("\n---- START Action Get Transactions (router) ----");

    try {
        let { user, message } = ctx.request.body;
        const [success, result] = await db.actionGetTransactions(user, message);

        if (success === false) {
            ctx.body = { error: result };
        } else {
            ctx.body = {
                transactions: result,
            };
        }
    } catch (error) {
        console.log("Get Transactions - ", error);
        ctx.status = 400;
        ctx.body = { error: error };
    }

    console.log("---- END Action Get Transactions (router) ----\n");
}

/**
 * Reads the payment message and extracts its components without verifying nothing.
 * @param {*} message Message with the payment information
 * @returns Array in the formar [user, products, discount, voucher, part, sign], where
 *      - user - id of the user that is paying
 *      - products - array with objects that represent a product ({id,price})
 *      - discount - true if discount is used and false otherwise
 *      - voucher - id of the voucher to be used, null otherwise
 *      - part - part of the message not including the signature
 *      - sign - signature of the user
 */
function readPayment(message) {
    const buf = Buffer.from(message, "base64");
    let offset = 0;

    // read UUID (16 bytes)
    let user = [
        buf.readBigUInt64BE(offset).toString(16).padStart(16, "0"),
        buf
            .readBigUInt64BE(offset + 8)
            .toString(16)
            .padStart(16, "0"),
    ].join("-");
    offset += 16;
    console.log("\nUserID -", user);

    // read product count (1 byte)
    const numberProducts = buf.readUInt8(offset);
    offset += 1;

    // read each product UUID and price (16 + 2 bytes)
    const products = [];
    console.log("\nProducts");
    for (let i = 0; i < numberProducts; i++) {
        const productId = [
            buf.readBigUInt64BE(offset).toString(16).padStart(16, "0"),
            buf
                .readBigUInt64BE(offset + 8)
                .toString(16)
                .padStart(16, "0"),
        ].join("-");
        offset += 16;
        const price = parseFloat(buf.readFloatBE(offset).toFixed(2));
        offset += 4;
        products.push({ productId: productId, price: price });
        console.log("   - ProductId and Price -", productId, price);
    }

    // discount flag (1 byte)
    const discount = buf.readUInt8(offset) === 1;
    offset += 1;
    console.log("\nDiscount -", discount);

    const useVoucher = buf.readUInt8(offset) === 1;
    offset += 1;
    console.log("\nUse voucher -", useVoucher);

    // voucher UUID (optional)
    let voucher = null;
    if (useVoucher === true) {
        voucher = [
            buf.readBigUInt64BE(offset).toString(16).padStart(16, "0"),
            buf
                .readBigUInt64BE(offset + 8)
                .toString(16)
                .padStart(16, "0"),
        ].join("-");
        offset += 16;
        console.log("Voucher -", voucher);
    }

    // extract message portion and signature portion
    const part = buf.slice(0, offset);
    const sign = buf.subarray(offset);

    // format user and voucher UUID
    user = formatStringToUUID(user);
    if (voucher !== null) voucher = formatStringToUUID(voucher);

    return [user, products, discount, voucher, part, sign];
}

/**
 * Converts a string to the UUID format
 * @param {String} str
 * @returns
 */
function formatStringToUUID(str) {
    return str
        .replace("-", "")
        .replace(
            /([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{12})/,
            "$1-$2-$3-$4-$5"
        );
}

/**
 * Starts automatic cleanup loop
 * @param {Integer} intervalMs - interval in milliseconds for the cleanup loop
 */
async function startAutoCleanup(intervalMs = 60 * 1000) {
    setInterval(() => {
        db.removeExpiredNonces().catch(console.error);
    }, intervalMs);
}

module.exports = router;
