package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationDocument;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {

    Page<NotificationDocument> findByRecipientUserIdAndStatusNot(Long recipientUserId, NotificationStatus status, Pageable pageable);
}

