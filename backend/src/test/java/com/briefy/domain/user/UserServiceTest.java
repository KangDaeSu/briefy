package com.briefy.domain.user;

import com.briefy.common.BriefyErrorCode;
import com.briefy.common.exception.BriefyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private User makeUser(UUID id) {
        User user = new User("tester@test.com", "Tester", AuthProvider.LOCAL, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void findById_success() {
        UUID id = UUID.randomUUID();
        User user = makeUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo("tester@test.com");
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.USER_NOT_FOUND));
    }

    @Test
    void updateName_success() {
        UUID id = UUID.randomUUID();
        User user = makeUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.updateName(id, "New Name");

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void delete_callsRepository() {
        UUID id = UUID.randomUUID();
        User user = makeUser(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.delete(id);

        verify(userRepository).delete(user);
    }
}
