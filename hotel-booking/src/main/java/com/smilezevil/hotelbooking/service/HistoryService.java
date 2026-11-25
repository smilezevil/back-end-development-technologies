package com.smilezevil.hotelbooking.service;

import com.smilezevil.hotelbooking.audit.AuditRevision;
import com.smilezevil.hotelbooking.dto.RevisionDTO;
import com.smilezevil.hotelbooking.entity.Booking;
import com.smilezevil.hotelbooking.entity.Hotel;
import com.smilezevil.hotelbooking.entity.Room;
import com.smilezevil.hotelbooking.exception.ResourceNotFoundException;
import com.smilezevil.hotelbooking.mapper.BookingMapper;
import com.smilezevil.hotelbooking.mapper.HotelMapper;
import com.smilezevil.hotelbooking.mapper.RoomMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final EntityManager entityManager;
    private final HotelMapper hotelMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public List<RevisionDTO> hotelHistory(Long id) {
        return history(Hotel.class, id, hotelMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO> roomHistory(Long id) {
        return history(Room.class, id, roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RevisionDTO> bookingHistory(Long id) {
        return history(Booking.class, id, bookingMapper::toDto);
    }

    private <T> List<RevisionDTO> history(Class<T> type, Long id, Function<T, Object> toSnapshot) {
        List<?> rows = AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Історії змін для " + type.getSimpleName() + " #" + id + " немає");
        }
        return rows.stream()
                .map(row -> toRevision((Object[]) row, type, toSnapshot))
                .toList();
    }

    private <T> RevisionDTO toRevision(Object[] row, Class<T> type, Function<T, Object> toSnapshot) {
        AuditRevision revision = (AuditRevision) row[1];
        RevisionType revisionType = (RevisionType) row[2];
        return new RevisionDTO(
                revision.getId(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(revision.getTimestamp()), ZoneId.systemDefault()),
                revision.getChangedBy(),
                revisionType.name(),
                toSnapshot.apply(type.cast(row[0])));
    }
}