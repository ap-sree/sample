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

SELECT 
    OBJECT_SCHEMA_NAME(t.object_id) AS SchemaName,
    t.name AS ObjectName,
    t.type_desc AS ObjectType,
    c.name AS ColumnName,
    ty.name + 
        CASE 
            WHEN ty.name IN ('varchar', 'char', 'varbinary', 'binary') 
                THEN '(' + CASE WHEN c.max_length = -1 THEN 'MAX' ELSE CAST(c.max_length AS VARCHAR) END + ')'
            WHEN ty.name IN ('nvarchar', 'nchar') 
                THEN '(' + CASE WHEN c.max_length = -1 THEN 'MAX' ELSE CAST(c.max_length/2 AS VARCHAR) END + ')'
            WHEN ty.name IN ('decimal', 'numeric') 
                THEN '(' + CAST(c.precision AS VARCHAR) + ',' + CAST(c.scale AS VARCHAR) + ')'
            ELSE ''
        END AS DataType,
    c.is_nullable AS IsNullable,
    -- Primary Key
    pk.name AS PrimaryKeyName,
    -- Foreign Key
    fk.name AS ForeignKeyName,
    OBJECT_NAME(fkc.referenced_object_id) AS ReferencedTable,
    rc.name AS ReferencedColumn,
    -- Default Constraint
    dc.name AS DefaultConstraintName,
    dc.definition AS DefaultValue,
    -- Check Constraint
    cc.name AS CheckConstraintName,
    cc.definition AS CheckConstraintDefinition,
    -- Unique Constraint
    uc.name AS UniqueConstraintName,
    -- Triggers (aggregated)
    trig.TriggerNames,
    trig.TriggerTypes,
    trig.TriggerStatuses
FROM sys.objects t
INNER JOIN sys.columns c ON t.object_id = c.object_id
INNER JOIN sys.types ty ON c.user_type_id = ty.user_type_id
-- Primary Key
LEFT JOIN sys.index_columns ic ON c.object_id = ic.object_id AND c.column_id = ic.column_id
LEFT JOIN sys.indexes pk ON ic.object_id = pk.object_id AND ic.index_id = pk.index_id AND pk.is_primary_key = 1
-- Foreign Key
LEFT JOIN sys.foreign_key_columns fkc ON c.object_id = fkc.parent_object_id AND c.column_id = fkc.parent_column_id
LEFT JOIN sys.foreign_keys fk ON fkc.constraint_object_id = fk.object_id
LEFT JOIN sys.columns rc ON fkc.referenced_object_id = rc.object_id AND fkc.referenced_column_id = rc.column_id
-- Default Constraint
LEFT JOIN sys.default_constraints dc ON c.default_object_id = dc.object_id
-- Check Constraint (table level)
LEFT JOIN sys.check_constraints cc ON t.object_id = cc.parent_object_id
-- Unique Constraint
LEFT JOIN sys.indexes uc ON c.object_id = uc.object_id AND uc.is_unique_constraint = 1
LEFT JOIN sys.index_columns uic ON uc.object_id = uic.object_id AND uc.index_id = uic.index_id AND c.column_id = uic.column_id
-- Triggers (aggregated per table/view)
OUTER APPLY (
    SELECT 
        STRING_AGG(tr.name, ', ') AS TriggerNames,
        STRING_AGG(tr.type_desc, ', ') AS TriggerTypes,
        STRING_AGG(CASE tr.is_disabled WHEN 0 THEN 'Enabled' ELSE 'Disabled' END, ', ') AS TriggerStatuses
    FROM sys.triggers tr
    WHERE tr.parent_id = t.object_id
) trig
WHERE t.type IN ('U', 'V')  -- U = User Table, V = View
ORDER BY t.type_desc, t.name, c.column_id;

SELECT 
    OBJECT_SCHEMA_NAME(f.object_id) AS SchemaName,
    f.name AS FunctionName,
    f.type_desc AS FunctionType,
    -- Input parameters
    STUFF((
        SELECT ', ' + p2.name + ' ' + TYPE_NAME(p2.user_type_id) +
            CASE 
                WHEN TYPE_NAME(p2.user_type_id) IN ('varchar', 'char', 'varbinary', 'binary') 
                    THEN '(' + CASE WHEN p2.max_length = -1 THEN 'MAX' ELSE CAST(p2.max_length AS VARCHAR) END + ')'
                WHEN TYPE_NAME(p2.user_type_id) IN ('nvarchar', 'nchar') 
                    THEN '(' + CASE WHEN p2.max_length = -1 THEN 'MAX' ELSE CAST(p2.max_length/2 AS VARCHAR) END + ')'
                WHEN TYPE_NAME(p2.user_type_id) IN ('decimal', 'numeric') 
                    THEN '(' + CAST(p2.precision AS VARCHAR) + ',' + CAST(p2.scale AS VARCHAR) + ')'
                ELSE ''
            END
        FROM sys.parameters p2
        WHERE p2.object_id = f.object_id AND p2.parameter_id > 0
        ORDER BY p2.parameter_id
        FOR XML PATH('')
    ), 1, 2, '') AS InputParameters,
    -- Return type for scalar functions
    (SELECT TYPE_NAME(p.user_type_id) +
        CASE 
            WHEN TYPE_NAME(p.user_type_id) IN ('varchar', 'char', 'varbinary', 'binary') 
                THEN '(' + CASE WHEN p.max_length = -1 THEN 'MAX' ELSE CAST(p.max_length AS VARCHAR) END + ')'
            WHEN TYPE_NAME(p.user_type_id) IN ('nvarchar', 'nchar') 
                THEN '(' + CASE WHEN p.max_length = -1 THEN 'MAX' ELSE CAST(p.max_length/2 AS VARCHAR) END + ')'
            WHEN TYPE_NAME(p.user_type_id) IN ('decimal', 'numeric') 
                THEN '(' + CAST(p.precision AS VARCHAR) + ',' + CAST(p.scale AS VARCHAR) + ')'
            ELSE ''
        END
     FROM sys.parameters p 
     WHERE p.object_id = f.object_id AND p.parameter_id = 0
    ) AS ScalarReturnType,
    -- Return columns for table-valued functions
    STUFF((
        SELECT ', ' + c2.name + ' ' + TYPE_NAME(c2.user_type_id) +
            CASE 
                WHEN TYPE_NAME(c2.user_type_id) IN ('varchar', 'char', 'varbinary', 'binary') 
                    THEN '(' + CASE WHEN c2.max_length = -1 THEN 'MAX' ELSE CAST(c2.max_length AS VARCHAR) END + ')'
                WHEN TYPE_NAME(c2.user_type_id) IN ('nvarchar', 'nchar') 
                    THEN '(' + CASE WHEN c2.max_length = -1 THEN 'MAX' ELSE CAST(c2.max_length/2 AS VARCHAR) END + ')'
                WHEN TYPE_NAME(c2.user_type_id) IN ('decimal', 'numeric') 
                    THEN '(' + CAST(c2.precision AS VARCHAR) + ',' + CAST(c2.scale AS VARCHAR) + ')'
                ELSE ''
            END
        FROM sys.columns c2
        WHERE c2.object_id = f.object_id
        ORDER BY c2.column_id
        FOR XML PATH('')
    ), 1, 2, '') AS TableReturnColumns,
    f.create_date AS CreateDate,
    f.modify_date AS ModifyDate
FROM sys.objects f
WHERE f.type IN ('FN', 'IF', 'TF', 'FS', 'FT')
ORDER BY f.name;