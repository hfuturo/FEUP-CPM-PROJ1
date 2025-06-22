"use strict";
const Koa = require("koa");
const logger = require("koa-logger");
const bodyParser = require("koa-bodyparser");
const router = require("./middleware/router");

const PORT = 8000;
const app = new Koa();

app.use(logger());
app.use(bodyParser({ strict: false }));
app.use(router.routes());
app.use(router.allowedMethods());
app.listen(PORT, () => console.log("Server running on port %d", PORT));
