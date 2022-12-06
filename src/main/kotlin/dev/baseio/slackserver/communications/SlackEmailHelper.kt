package dev.baseio.slackserver.communications

import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object SlackEmailHelper {
    fun sendEmail(toEmail: String,link:String) {
        // Get a Properties object
        val props: Properties = System.getProperties()
        props.setProperty("mail.smtp.host", "smtp.gmail.com")
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        props.setProperty("mail.smtp.socketFactory.fallback", "false")
        props.setProperty("mail.smtp.port", "465")
        props.setProperty("mail.smtp.socketFactory.port", "465")
        props["mail.smtp.auth"] = "true"
        val username = System.getenv("email.username")
        val password = System.getenv("email.password")
        val session: Session = Session.getDefaultInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        // -- Create a new message --
        val msg: Message = MimeMessage(session)

        // -- Set the FROM and TO fields --
        msg.setFrom(InternetAddress(System.getenv("email.from")))
        msg.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(toEmail, false)
        )
        msg.subject = "Confirm your email address on SlackClone"
        msg.setText(
            "Confirm your email address to get started on SlackClone\n" +
                    "Once you’ve confirmed that $toEmail is your email address, we’ll help you find your SlackClone workspaces or create a new one.\n" +
                    "\n" + "\uD83D\uDCF1 From your mobile device, navigate the link below to confirm:\n" +
                    "$link\n" + "If you didn’t request this email, there’s nothing to worry about — you can safely ignore it.\n" +
                    "\n"
        )
        msg.sentDate = Date()
        Transport.send(msg)
    }
}