ALTER TABLE outbox_evento
    ADD CONSTRAINT ck_outbox_evento_payload_tamanho CHECK (octet_length(payload) <= 524288);

COMMENT ON COLUMN outbox_evento.payload IS 'Evento serializado em JSON, publicado como valor da mensagem; limitado a 512 KB pela constraint ck_outbox_evento_payload_tamanho, abaixo do limite de 1 MB do Kafka, para que nenhum escritor (aplicação ou script) grave um evento impossível de publicar';
