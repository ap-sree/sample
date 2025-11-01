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