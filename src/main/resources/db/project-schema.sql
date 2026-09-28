-- 已批准的项目持久化四表；仅在核实目标库后显式执行，应用不自动执行DDL。
-- 不包含凭据、不建库、不删除既有表。

CREATE TABLE zb_agent (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creation_data DATETIME(3) NOT NULL,
    lastupdate_data DATETIME(3) NOT NULL,
    del_flg BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    brand VARCHAR(255),
    name VARCHAR(255),
    ver VARCHAR(255),
    think VARCHAR(255),
    role_prompt LONGTEXT,
    skill_name VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE zb_project (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creation_data DATETIME(3) NOT NULL,
    lastupdate_data DATETIME(3) NOT NULL,
    del_flg BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    project_id CHAR(36) NOT NULL,
    project_name TEXT,
    planning_agent_id BIGINT NOT NULL,
    development_agent_id BIGINT NOT NULL,
    review_agent_id BIGINT NOT NULL,
    project_prompt LONGTEXT,
    UNIQUE KEY uk_zb_project_uuid (project_id),
    FOREIGN KEY (planning_agent_id) REFERENCES zb_agent(id),
    FOREIGN KEY (development_agent_id) REFERENCES zb_agent(id),
    FOREIGN KEY (review_agent_id) REFERENCES zb_agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE zb_requirement (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creation_data DATETIME(3) NOT NULL,
    lastupdate_data DATETIME(3) NOT NULL,
    del_flg BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    project_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    user_content LONGTEXT,
    agent_understanding LONGTEXT,
    acceptance_criteria LONGTEXT,
    user_confirm_msg LONGTEXT,
    FOREIGN KEY (project_id) REFERENCES zb_project(id),
    UNIQUE KEY uk_zb_requirement_order (project_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE zb_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creation_data DATETIME(3) NOT NULL,
    lastupdate_data DATETIME(3) NOT NULL,
    del_flg BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    requirement_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    content LONGTEXT,
    acceptance_criteria LONGTEXT,
    status VARCHAR(32) NOT NULL,
    result_json JSON,
    FOREIGN KEY (requirement_id) REFERENCES zb_requirement(id),
    UNIQUE KEY uk_zb_task_order (requirement_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
