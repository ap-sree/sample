SELECT 
    t.name AS TableName,
    c.name AS ColumnName,
    dc.name AS DefaultConstraintName,
    dc.definition AS DefaultValue,
    cc.name AS CheckConstraintName,
    cc.definition AS CheckConstraintDefinition
FROM sys.tables t
INNER JOIN sys.columns c ON t.object_id = c.object_id
LEFT JOIN sys.default_constraints dc ON c.default_object_id = dc.object_id
LEFT JOIN sys.check_constraints cc ON t.object_id = cc.parent_object_id
ORDER BY t.name, c.column_id;

SELECT 
    t.name AS TableName,
    c.name AS ColumnName,
    ty.name AS DataType,
    c.max_length AS MaxLength,
    c.precision AS Precision,
    c.scale AS Scale,
    c.is_nullable AS IsNullable,
    dc.name AS DefaultConstraintName,
    dc.definition AS DefaultValue,
    cc.name AS CheckConstraintName,
    cc.definition AS CheckConstraintDefinition
FROM sys.tables t
INNER JOIN sys.columns c ON t.object_id = c.object_id
INNER JOIN sys.types ty ON c.user_type_id = ty.user_type_id
LEFT JOIN sys.default_constraints dc ON c.default_object_id = dc.object_id
LEFT JOIN sys.check_constraints cc ON t.object_id = cc.parent_object_id
ORDER BY t.name, c.column_id;