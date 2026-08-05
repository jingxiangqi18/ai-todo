CREATE TABLE users(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(25) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE tasks(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(50) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_tasks_user_id (user_id),
    INDEX idx_tasks_user_status (user_id, status),
    CONSTRAINT fk_tasks_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE task_steps(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_task_steps_task_id (task_id),
    CONSTRAINT fk_task_steps_task_id
    FOREIGN KEY (task_id) REFERENCES tasks(id)
    ON DELETE CASCADE
);

CREATE TABLE ai_call_logs(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    feature VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL,
    success TINYINT(1) NOT NULL,
    error_message VARCHAR(300) NULL,
    created_at DATETIME NOT NULL,

    INDEX idx_ai_call_logs_user_created_at (user_id, created_at),
    INDEX idx_ai_call_logs_feature_created_at (feature, created_at),

    CONSTRAINT fk_ai_call_logs_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE task_groups(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_task_groups_owner_name
    UNIQUE (owner_id, name),

    CONSTRAINT fk_task_groups_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
);

CREATE TABLE task_group_members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_task_group_members_group_user
        UNIQUE (group_id, user_id),

    CONSTRAINT fk_task_group_members_group
        FOREIGN KEY (group_id)
        REFERENCES task_groups(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_group_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_task_group_members_user_id
        (user_id)
);

CREATE TABLE task_group_invitations(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME DEFAULT NULL,

    CONSTRAINT fk_task_group_invitations_group
        FOREIGN KEY (group_id)
        REFERENCES task_groups(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_group_invitations_inviter
        FOREIGN KEY (inviter_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_group_invitations_invitee
        FOREIGN KEY (invitee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    INDEX idx_task_group_invitations_invitee_status
        (invitee_id, status),

    INDEX idx_task_group_invitations_inviter_id
        (inviter_id)
);