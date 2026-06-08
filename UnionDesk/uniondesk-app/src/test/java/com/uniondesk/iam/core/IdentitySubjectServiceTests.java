package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.iam.entity.IdentitySubjectPo;
import com.uniondesk.iam.repository.IdentitySubjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentitySubjectServiceTests {

    @Mock
    private IdentitySubjectRepository identitySubjectRepository;

    private IdentitySubjectService service;

    @BeforeEach
    void setUp() {
        service = new IdentitySubjectService(identitySubjectRepository);
    }

    @Test
    void resolveSubjectIdByPhoneReturnsExisting() {
        when(identitySubjectRepository.findIdByPhone("13800000000")).thenReturn(Optional.of(9L));
        when(identitySubjectRepository.findMergedIntoId(9L)).thenReturn(Optional.empty());

        assertThat(service.resolveSubjectIdByPhone("13800000000")).isEqualTo(9L);
    }

    @Test
    void resolveEffectiveSubjectIdFollowsMergeChain() {
        when(identitySubjectRepository.findMergedIntoId(1L)).thenReturn(Optional.of(2L));
        when(identitySubjectRepository.findMergedIntoId(2L)).thenReturn(Optional.empty());

        assertThat(service.resolveEffectiveSubjectId(1L)).isEqualTo(2L);
    }

    @Test
    void requireActiveSubjectRejectsMergedSubject() {
        IdentitySubjectPo row = new IdentitySubjectPo();
        row.setId(3L);
        row.setStatus("active");
        row.setMergedIntoId(4L);
        when(identitySubjectRepository.findMergedIntoId(3L)).thenReturn(Optional.empty());
        when(identitySubjectRepository.findById(3L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.requireActiveSubject(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("身份主体不可用");
    }

    @Test
    void resolveSubjectIdByPhoneCreatesWhenMissing() {
        when(identitySubjectRepository.findIdByPhone("13900000000")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            IdentitySubjectPo po = invocation.getArgument(0);
            po.setId(100L);
            return null;
        }).when(identitySubjectRepository).insert(any(IdentitySubjectPo.class));

        assertThat(service.resolveSubjectIdByPhone("13900000000")).isEqualTo(100L);
        verify(identitySubjectRepository).insert(any(IdentitySubjectPo.class));
    }
}
