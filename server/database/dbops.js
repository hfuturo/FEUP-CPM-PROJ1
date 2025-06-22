"use strict";
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const { v4: uuidv4 } = require("uuid");
const groceries_data = require("../groceries/groceries_info.json");

// path to the SQLite database
const DB_PATH = path.resolve("database/users.db");

class DBOps {
    constructor() {
        this.db = null;
    }

    async init() {
        if (!this.db) {
            const { Database } = await import("sqlite-async");
            const dbExists = fs.existsSync(DB_PATH);
            this.db = await Database.open(DB_PATH);
            if (!dbExists) {
                await this.createTables();
            }
        }
    }

    /**
     * Creates the 'Users' table if it doesn't exist in the database.
     * The table stores the UUID, EC public key, and RSA public key of users.
     * @returns {Promise<void>} - Returns a Promise that resolves when the tables are created.
     */
    async createTables() {
        try {
            const tableUser = `
                CREATE TABLE IF NOT EXISTS Users (
                Uuid UUID PRIMARY KEY, 
                KeyEC TEXT NOT NULL, 
                KeyRSA TEXT NOT NULL,
                CardNumber TEXT NOT NULL, 
                CardDate TEXT NOT NULL, 
                SelectedCardType TEXT NOT NULL,
                Current REAL DEFAULT 0,
                Discount REAL DEFAULT 0
            );`;

            const tableVoucher = `
                CREATE TABLE IF NOT EXISTS Vouchers (
                Uuid UUID,
                UserUuid UUID,
                PRIMARY KEY (Uuid, UserUuid),
                FOREIGN KEY (UserUuid) REFERENCES Users(Uuid) ON DELETE CASCADE
            );`;

            const tableNonce = `
                CREATE TABLE IF NOT EXISTS Nonce (
                Uuid UUID,
                UserUuid UUID,
                Timestamp INTEGER,
                Type TEXT,
                PRIMARY KEY (Uuid, UserUuid),
                FOREIGN KEY (UserUuid) REFERENCES Users(Uuid) ON DELETE CASCADE
            );`;

            const tableTransactions = `
                CREATE TABLE IF NOT EXISTS Transactions(
                Uuid UUID,
                Price REAL,
                Date DATETIME NOT NULL DEFAULT (strftime('%d-%m-%Y %H:%M', 'now', 'localtime')),
                UserUuid UUID,
                PRIMARY KEY (Uuid, UserUuid),
                FOREIGN KEY (UserUuid) REFERENCES Users(Uuid) ON DELETE CASCADE
            );`;

            const tableGroceries = `
                CREATE TABLE IF NOT EXISTS Grocery (
                Name TEXT PRIMARY KEY,
                Category TEXT NOT NULL,
                SubCategory TEXT NOT NULL,
                Description TEXT NOT NULL,
                ImagePath TEXT NOT NULL,
                Price REAL NOT NULL
            );`;

            await this.db.run(tableUser);
            await this.db.run(tableVoucher);
            await this.db.run(tableNonce);
            await this.db.run(tableTransactions);
            await this.db.run(tableGroceries);

            await this.populateGroceries();
            console.log("Success in the database initialization!");

            const check = await this.db.all(
                "SELECT name FROM sqlite_master WHERE type='table'"
            );
            console.log("Tables in DB:", check);
        } catch (error) {
            console.log("Failure in the database initialization!", error);
        }
    }

    async populateGroceries() {
        console.log("---- START Populating Grocery Table (db) ----");
        try {
            for (const grocery_info of groceries_data) {
                await this.insertGroceryIntoTable(grocery_info);
            }
        } catch (error) {
            console.log("Error populating Grocery table");
            console.log(error)
            return null;
        }
        console.log("---- END Populating Grocery Table (db) ----");
    }

    async insertGroceryIntoTable(grocery_info) {
        await this.db.run(
            `
            INSERT INTO Grocery (Name, Category, SubCategory, Description, ImagePath, Price)
            VALUES (?, ?, ?, ?, ?, ?)
            `,
            [
                grocery_info["name"],
                grocery_info["category"],
                grocery_info["sub_category"],
                grocery_info["description"],
                grocery_info["image_path"],
                grocery_info["price"],
            ]
        );
    }

    async getGroceries() {
        console.log("---- START Action Get Grocery (db) ----");

        let result;
        try {
            const rows = await this.db.all(`SELECT * FROM Grocery`);

            if (!rows || rows.length === 0) {
                throw new Error("Error obtaining groceries");
            }

            result = [true, rows];
        } catch (error) {
            result = [false, error];
        }

        console.log("---- END Action Get Groceries (db) ----");
        return result;
    }

    /**
     * Checks the existence of an user according with his Id.
     * @param {*} user Id of the user to be retrieve
     * @returns Array of size 2 where the first element indicates the success (Boolean) and the second the error if exists
     */
    async actionGetUser(user) {
        console.log("---- START Action Get User (db) ----");

        let result;
        try {
            const row = await this.db.get(
                `SELECT * FROM Users WHERE Uuid = ?`,
                [user]
            );
            if (!row) throw new Error("User not found!");

            result = [true, null];
        } catch (error) {
            result = [false, error];
        }

        console.log("---- END Action Get User (db) ----");
        return result;
    }

    async getUserDiscount(userId) {
        try {
            // retrieve the EC public key from the database for the specified user
            const row = await this.db.get(
                `SELECT Discount FROM Users WHERE Uuid = ?`,
                [userId]
            );
            if (!row) {
                return null;
            }
            return row.Discount;
        } catch (error) {
            console.log("Error in discount fetch!");
            return null;
        }
    }

    async actionGetVouchers(user, message) {
        try {
            const [success, result] = await this.verifyNonce(
                message,
                user,
                "VOUCHER"
            );

            if (success === false) {
                throw new Error(result);
            }

            // check if there are vouchers to fetch
            const rows = await this.db.all(
                `SELECT * FROM Vouchers WHERE UserUuid = ?`,
                [user]
            );

            if (!rows || rows.length === 0) {
                return [true, []];
            }

            let vouchers = [];
            for (const row of rows) {
                vouchers.push({ uuid: row.Uuid });
            }
            return [true, vouchers];
        } catch (error) {
            console.log("Error in voucher fetch!", error);
            return [false, error];
        }
    }

    async getUserDiscount(user) {
        try {
            const row = await this.db.get(
                `SELECT Discount FROM Users WHERE Uuid = ?`,
                [user]
            );

            if (!row) {
                return [true, 0];
            }
            return [true, row.Discount];
        } catch (error) {
            console.log("Error in discount fetch!", error);
            return [false, error];
        }
    }

    async actionGetTransactions(user, message) {
        try {
            const [success, result] = await this.verifyNonce(
                message,
                user,
                "TRANSACTION"
            );

            if (success === false) {
                throw new Error(result);
            }

            const rows = await this.db.all(
                `SELECT * FROM Transactions WHERE UserUuid = ?`,
                [user]
            );

            if (!rows || rows.length === 0) {
                return [true, []];
            }

            let transactions = [];
            for (const row of rows) {
                transactions.push({
                    uuid: row.Uuid,
                    price: row.Price,
                    date: row.Date,
                });
            }
            return [true, transactions];
        } catch (error) {
            console.log("Error in transaction fecth!", error);
            return [false, error];
        }
    }

    async actionAddNonce(user, type) {
        try {
            const timestamp = Date.now();
            const uuid = uuidv4();
            await this.db.run(
                `
                INSERT OR IGNORE INTO Nonce (UserUuid, Uuid, Timestamp, Type)
                VALUES (?, ?, ?, ?)
            `,
                [user, uuid, timestamp, type]
            );
            return [true, uuid];
        } catch (error) {
            return [false, error];
        }
    }

    /**
     * Adds a new user to the database with their EC and RSA public keys.
     * @param {string} keyEC The EC public key encoded in Base64.
     * @param {string} keyRSA The RSA public key encoded in Base64.
     * @param {string} name The name of the user.
     * @param {string} nick The nickname of the user.
     * @param {string} cardNumber The number (in string) of the user's card.
     * @param {string} cardDate The experiration date (in string) of the user's card
     * @param {string} selectedCardType The type of the user's card.
     * @returns {Promise<[string, Object]>} - Returns the user's UUID and the result of the database operation.
     */
    async actionRegistration(
        keyEC,
        keyRSA,
        cardNumber,
        cardDate,
        selectedCardType
    ) {
        let result;
        let uuid;
        let row = null;
        try {
            uuid = uuidv4();
            result = await this.db.run(
                `
                INSERT OR IGNORE INTO Users (Uuid, KeyEC, KeyRSA, CardNumber, CardDate, SelectedCardType)
                VALUES (?, ?, ?, ?, ?, ?)
            `,
                [
                    uuid,
                    keyEC,
                    keyRSA,
                    cardNumber,
                    cardDate,
                    selectedCardType,
                ]
            );

            row = await this.db.get(
                `
                SELECT Uuid FROM Users WHERE KeyEC = ? AND KeyRSA = ?
            `,
                [keyEC, keyRSA]
            );
            if (result.changes === 0) result = false;
        } catch (error) {
            console.log(error);
            result = false;
        }
        if (row === null) {
            return [null, false];
        }
        return [row.Uuid, true];
    }

    async actionPayment(user, voucher, usedDiscount, total, discount) {
        console.log("---- START Payment Transaction (db) ----");
        await this.db.run("BEGIN");
        try {
            // delete voucher if it was used
            if (voucher !== null) {
                console.log("Deleting used voucher.");
                await this.db.run(
                    `DELETE FROM Vouchers WHERE Uuid = ? AND UserUuid = ?`,
                    [voucher, user]
                );
            }

            // update user according with the use of discount
            console.log("Update user.");
            await this.db.run(
                `UPDATE Users SET Current = Current + ? , Discount = Discount - ? + ? WHERE Uuid = ?`,
                [total, usedDiscount, discount, user]
            );

            await this.db.run("COMMIT");
            console.log("Success in payment transaction 1!");
        } catch (error) {
            await this.db.run("ROLLBACK");
            console.log("Failure in payment transaction 1!", error);
            console.log("---- END Payment Transaction (db) ----");
            return false;
        }

        await this.db.run("BEGIN");
        try {
            // check if id current divisible by 100
            const row = await this.db.get(
                `SELECT Current FROM Users WHERE Uuid = ?`,
                [user]
            );
            if (!row) throw new Error("User not found!");

            const current = row.Current / 100;
            const numberVouchers = Math.floor(current);

            if (numberVouchers > 0) {
                console.log("Add new voucher.");
                // add new vouchers to the user
                for (let i = 0; i < numberVouchers; i++) {
                    const result = await this.db.run(
                        "insert into Vouchers(Uuid, UserUuid) values(?,?)",
                        [uuidv4(), user]
                    );
                    if (result.changes === 0)
                        throw new Error("result.changes === 0");
                }

                // update current in the user
                await this.db.run(
                    `UPDATE Users SET Current = ? WHERE Uuid = ?`,
                    [current % 100, user]
                );
            }
            await this.db.run("COMMIT");
            console.log("Success in payment transaction 2!");
        } catch (error) {
            await this.db.run("ROLLBACK");
            console.log("Failure in payment transaction 2!", error);
            console.log("---- END Payment Transaction (db) ----");
            return false;
        }

        await this.db.run("BEGIN");
        try {
            const uuid = uuidv4();
            await this.db.run(
                `
                INSERT INTO Transactions (Uuid, Price, UserUuid)
                VALUES (?, ?, ?)
                `,
                [uuid, total, user]
            );

            await this.db.run("COMMIT");
            console.log("Success in payment transaction 3!");

            return true;
        } catch (error) {
            await this.db.run("ROLLBACK");
            console.log("Failure in payment transaction 3!", error);
            return false;
        } finally {
            console.log("---- END Payment Transaction (db) ----");
        }
    }

    /**
     * Verifies the ECDSA signature of a payment message.
     * Uses the EC public key stored in the database to verify the signature.
     * @param {string} userId The UUID of the user making the payment.
     * @param {Buffer} signature The signature of the message.
     * @param {Buffer} messageContent The content of the message that was signed.
     * @returns {Promise<boolean>} Returns 'true' if the signature is valid, 'false' otherwise.
     */
    async verifySignature(userId, signature, messageContent) {
        try {
            // retrieve the EC public key from the database for the specified user
            const row = await this.db.get(
                `SELECT KeyEC FROM Users WHERE Uuid = ?`,
                [userId]
            );
            if (!row) throw new Error("User not found!");

            const publicKeyString = row.KeyEC;
            const buffer = Buffer.from(publicKeyString, "base64");

            // verify the signature using the public key and message content
            const publicKey = crypto.createPublicKey({
                key: buffer,
                format: "der",
                type: "spki",
            });

            const verified = crypto.verify(
                "sha256",
                messageContent,
                publicKey,
                signature
            );

            return verified;
        } catch (error) {
            console.log("ERROR:", error);
            console.log("Error in signature verification!");
            return false;
        }
    }

    async verifyVoucher(userId, voucherId) {
        try {
            const row = await this.db.get(
                `SELECT Uuid FROM Vouchers WHERE Uuid = ? AND UserUuid = ?`,
                [voucherId, userId]
            );
            if (!row) throw new Error("No match");
            return true;
        } catch (error) {
            console.log("Error in voucher verification!");
            return false;
        }
    }

    async verifyNonce(message, user, type) {
        console.log("---- START Verify Nonce (db) ----");
        try {
            // getting public key
            let row = await this.db.get(
                `SELECT KeyRSA FROM Users WHERE Uuid = ?`,
                [user]
            );
            if (!row) {
                throw new Error("No match for user according with Uuid!");
            }

            // building public key
            const buf = Buffer.from(row.KeyRSA, "base64");
            const key = crypto.createPublicKey({
                key: buf,
                format: "der",
                type: "spki",
            });

            // getting nonce
            const limit = Date.now() - 2 * 60 * 1000;
            let rows = await this.db.all(
                `SELECT Uuid FROM Nonce WHERE UserUuid = ? AND Type = ? AND Timestamp >= ?`,
                [user, type, limit]
            );
            if (!rows || rows.length === 0) {
                throw new Error(
                    "No match for nonce according with UserUuid, type and timestamp!"
                );
            }

            console.log("Verifying Nonce.");

            // verifying if nonce is correct
            let nonce = crypto
                .publicDecrypt(
                    {
                        key: key,
                        padding: crypto.constants.RSA_PKCS1_PADDING,
                    },
                    Buffer.from(message, "base64")
                )
                .toString("hex");

            nonce = [
                nonce.slice(0, 8),
                nonce.slice(8, 12),
                nonce.slice(12, 16),
                nonce.slice(16, 20),
                nonce.slice(20),
            ].join("-");

            let correctNonce = false;
            for (const row of rows) {
                if (nonce.toString("hex") === row.Uuid) {
                    correctNonce = true;
                    await this.db.run(
                        `DELETE FROM Nonce WHERE Uuid = ? AND UserUuid = ? AND Type = ?`,
                        [row.Uuid, user, type]
                    );
                    break;
                }
            }
            if (correctNonce === false) {
                throw new Error("Nonce is incorrect!");
            }

            console.log("---- END Verify Nonce (db) ----");
            return [true, null];
        } catch (error) {
            console.log(error);
            console.log("---- END Verify Nonce (db) ----");
            return [false, error];
        }
    }

    // Removes expired nonces
    async removeExpiredNonces() {
        const limit = Date.now() - 2 * 60 * 1000;
        const result = await this.db.run(
            `DELETE FROM Nonce WHERE Timestamp < ?`,
            [limit]
        );
        console.log(`🧹 ${result.changes} expired nonces removed.`);
    }
}

module.exports = new DBOps();
