INSERT INTO roles (code, name, description)
VALUES ('super_admin', 'Super Admin', 'Full access to manage users, roles and audit logs'),
       ('editor', 'Editor', 'Can manage and publish articles'),
       ('contributor', 'Contributor', 'Can create and edit their own articles'),
       ('viewer', 'Viewer', 'Can view articles only')
ON CONFLICT (code) DO NOTHING;