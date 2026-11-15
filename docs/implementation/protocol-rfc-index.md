# Mail protocol RFC index

This index is a starting point for protocol implementation and review in GreenMail. It links protocol areas to their principal specifications, implementation entry points, and representative tests. It is not a claim of full RFC conformance: a registered command or a citation in code does not establish support for every requirement or extension in that RFC.

Use the RFC section that governs the behavior being changed, and check its current status at the [RFC Editor](https://www.rfc-editor.org/) or [IETF Datatracker](https://datatracker.ietf.org/). Older RFCs remain relevant when GreenMail implements a legacy protocol version; newer or superseding RFCs do not imply that GreenMail implements the newer version.

## Core protocols

| Area | Specification starting points | Implementation entry points | Representative tests |
| --- | --- | --- | --- |
| SMTP command exchange and message transfer | [RFC 5321](https://www.rfc-editor.org/rfc/rfc5321.html) | `../../greenmail-core/src/main/java/com/icegreen/greenmail/smtp/SmtpHandler.java`; `smtp/commands/SmtpCommandRegistry.java`; `smtp/commands/` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/smtp/SMTPCommandTest.java`; `SmtpServerTest.java` |
| SMTP authentication | [RFC 4954](https://www.rfc-editor.org/rfc/rfc4954.html) (SMTP AUTH); [RFC 7628](https://www.rfc-editor.org/rfc/rfc7628.html) (SASL OAuth mechanisms) | `smtp/commands/AuthCommand.java`; `util/SaslMessage.java`; `util/SaslXoauth2Message.java` | `SMTPCommandTest.java`; `SmtpSecureServerTest.java` |
| IMAP4rev1 commands, syntax, and session states | [RFC 3501](https://www.rfc-editor.org/rfc/rfc3501.html); see also [RFC 9051](https://www.rfc-editor.org/rfc/rfc9051.html), which specifies IMAP4rev2 and obsoletes RFC 3501 | `../../greenmail-core/src/main/java/com/icegreen/greenmail/imap/ImapHandler.java`; `imap/commands/ImapCommandFactory.java`; `imap/commands/` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/ImapServerTest.java`; `imap/commands/ImapProtocolTest.java`; `imap/commands/CommandParserTest.java` |
| IMAP authentication | [RFC 3501](https://www.rfc-editor.org/rfc/rfc3501.html) (LOGIN / AUTHENTICATE); [RFC 4959](https://www.rfc-editor.org/rfc/rfc4959.html) (SASL-IR extension) | `imap/commands/LoginCommand.java`; `imap/commands/AuthenticateCommand.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/commands/AuthenticateCommandTest.java` |
| IMAP IDLE | [RFC 2177](https://www.rfc-editor.org/rfc/rfc2177.html) | `imap/commands/IdleCommand.java`; `imap/commands/ImapCommandFactory.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/ImapIdleExpungeTest.java` |
| IMAP SORT | [RFC 5256](https://www.rfc-editor.org/rfc/rfc5256.html) | `imap/commands/SortCommand.java`; `imap/commands/SortCommandParser.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/ImapSortTest.java` |
| IMAP MOVE | [RFC 6851](https://www.rfc-editor.org/rfc/rfc6851.html) | `imap/commands/MoveCommand.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/commands/ImapProtocolTest.java` |
| IMAP QUOTA | [RFC 2087](https://www.rfc-editor.org/rfc/rfc2087.html) | `imap/commands/GetQuotaCommand.java`; `GetQuotaRootCommand.java`; `SetQuotaCommand.java`; `store/Store.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/QuotaRootNameQuotingTest.java`; `imap/commands/ImapProtocolTest.java` |
| IMAP UIDPLUS / UID operations | [RFC 4315](https://www.rfc-editor.org/rfc/rfc4315.html) | `imap/commands/ExpungeCommand.java`; `UidCommand.java`; `UidEnabledCommand.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/imap/ImapSequenceNumberExpungeTest.java`; `imap/commands/ImapProtocolTest.java` |
| POP3 commands and message retrieval | [RFC 1939](https://www.rfc-editor.org/rfc/rfc1939.html) | `../../greenmail-core/src/main/java/com/icegreen/greenmail/pop3/Pop3Handler.java`; `pop3/commands/Pop3CommandRegistry.java`; `pop3/commands/` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/pop3/POP3CommandTest.java`; `Pop3ServerTest.java` |
| POP3 capabilities and authentication | [RFC 2449](https://www.rfc-editor.org/rfc/rfc2449.html) (CAPA); [RFC 5034](https://www.rfc-editor.org/rfc/rfc5034.html) (POP3 SASL authentication) | `pop3/commands/CapaCommand.java`; `AuthCommand.java`; `Pop3CommandRegistry.java` | `../../greenmail-core/src/test/java/com/icegreen/greenmail/pop3/POP3CommandTest.java` |

Paths in the table are relative to `../../greenmail-core/src/main/java/com/icegreen/greenmail` unless prefixed with the full module path. Test paths are relative to `greenmail-core/src/test/java/com/icegreen/greenmail/`.

## Message format and shared behavior

SMTP, IMAP, and POP3 carry Internet messages; protocol behavior can therefore depend on message-format specifications as well as the transport RFC.

| Concern | Specification starting points | Implementation entry points | Representative tests |
| --- | --- | --- | --- |
| Internet message format and headers | [RFC 5322](https://www.rfc-editor.org/rfc/rfc5322.html) (updates and obsoletes RFC 822 / RFC 2822) | `../../greenmail-core/src/main/java/com/icegreen/greenmail/store`; Jakarta Mail message handling | `../../greenmail-core/src/test/java/com/icegreen/greenmail/specificmessages/Rfc822MessageTest.java`; `store/SimpleMessageAttributesAddressTest.java` |
| MIME content and transfer encodings | [RFC 2045](https://www.rfc-editor.org/rfc/rfc2045.html); [RFC 2046](https://www.rfc-editor.org/rfc/rfc2046.html) | `../../greenmail-core/src/main/java/com/icegreen/greenmail/store/SimpleMessageAttributes.java`; message storage and retrieval | `../../greenmail-core/src/test/java/com/icegreen/greenmail/store/SizeAndLineCountTest.java` |
| Encoded header words and content disposition | [RFC 2047](https://www.rfc-editor.org/rfc/rfc2047.html); [RFC 2183](https://www.rfc-editor.org/rfc/rfc2183.html) | `../../greenmail-core/src/main/java/com/icegreen/greenmail/store`; SMTP message handling | `../../greenmail-core/src/test/java/com/icegreen/greenmail/store/EnvelopeHeaderLineInjectionTest.java`; `store/ContentDispositionEscapeTest.java`; `smtp/SmtpReceivedHeaderInjectionTest.java` |

## Guidance for implementation and review

1. Start with the affected command or message behavior above; inspect both its implementation and the linked tests.
2. Read the specific RFC section and its normative language. Check errata, updates, and obsoletes, but do not silently apply a newer protocol version to GreenMail's existing behavior.
3. Treat this index as navigation, not a feature guarantee. Confirm advertised capabilities, supported states, configuration, and actual tests before concluding a feature is supported.
4. For a behavior change, add or update a focused protocol-level test for the wire response, state transition, and relevant boundary or malformed input. Update this index when a protocol area gains or changes a specification reference or representative test.

## Known reference caveats

- The implementation contains references to historical RFCs, including RFC 2060 (an early IMAP4rev1 specification), RFC 1734 (POP3 AUTH), and RFC 2554 (SMTP AUTH). For new work, use the current specification and its status; retain historical references only when they explain compatibility or existing behavior.
- RFC 9051 is useful when comparing IMAP behavior, but the presence of IMAP4rev1 code and tests does not establish IMAP4rev2 support.
- The test files listed are representative entry points, not exhaustive conformance suites. A test's presence does not imply every normative case is covered.
