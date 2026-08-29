ALTER TABLE whatsapp_onboarding
    ADD COLUMN sender_asociado BOOLEAN NOT NULL DEFAULT FALSE AFTER channel_sender_sid;
