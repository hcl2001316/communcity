package com.hcl.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {

    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    //发送的目标 发送的主题 发送的内容
    public void sendMail(String to, String subject, String content) {
        try {

            //构建MimeMessage
            MimeMessage message = mailSender.createMimeMessage();
            //帮助我们构建里面的内容
            MimeMessageHelper helper = new MimeMessageHelper(message);
            //发件人
            helper.setFrom(from);
            //收件人
            helper.setTo(to);
            //设置主题
            helper.setSubject(subject);
            //设置内容 允许支持html
            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败:" + e.getMessage());
        }
    }

}
