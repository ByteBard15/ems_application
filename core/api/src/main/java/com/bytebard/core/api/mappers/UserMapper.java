package com.bytebard.core.api.mappers;

import com.bytebard.core.api.models.User;
import com.bytebard.core.api.types.UserDTO;

public class UserMapper {

    public static UserDTO toUserDTO(User user) {
        if (user == null) return null;

        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
