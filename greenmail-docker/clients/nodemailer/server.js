// From https://nodemailer.com/about/#example

"use strict";
const nodemailer = require("nodemailer");

// async..await is not allowed in global scope, must use a wrapper
async function main() {
  // create reusable transporter object using the default SMTP transport
  let transporter = nodemailer.createTransport({
    host: "greenmail",
    port: 3025,
    secure: false,
    //auth: {
    //  user: 'foo',
    //  pass: 'bar',
    //},
    debug: true,
    logger: true,
  });

  // send mail with defined transport object
  let info = await transporter.sendMail({
    from: '"Fred Foo 👻" <foo@bar.com>', // sender address
    to: "foo2@bar.com, foo3@bar.com", // list of receivers
    subject: "Hello ✔", // Subject line
    text: "Hello world?", // plain text body
    html: "<b>Hello world?</b>", // html body
  });

  console.log("Message sent: %s", info.messageId);
}

main().catch(console.error);

