package com.fitplate.fitplateapi.weight.repository;

import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.weight.domain.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    Optional<WeightRecord> findByUserAndRecordDate(User user, LocalDate recordDate);

    List<WeightRecord> findByUserAndRecordDateBetweenOrderByRecordDateAsc(User user, LocalDate from, LocalDate to);

    Optional<WeightRecord> findFirstByUserOrderByRecordDateDesc(User user);
}
