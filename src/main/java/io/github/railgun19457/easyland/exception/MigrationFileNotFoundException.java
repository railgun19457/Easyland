package io.github.railgun19457.easyland.exception;

/**
 * 当执行数据迁移时，如果必需的旧数据文件不存在，则抛出此异常。
 */
public class MigrationFileNotFoundException extends Exception {
    
    private final String fileName;
    
    /**
     * 构造一个新的MigrationFileNotFoundException。
     *
     * @param fileName 不存在的文件名
     */
    public MigrationFileNotFoundException(String fileName) {
        super("迁移所需的文件 '" + fileName + "' 不存在。请确保旧版数据文件已正确放置在 oldfiles/ 目录中。");
        this.fileName = fileName;
    }
    
    /**
     * 获取导致异常的文件名。
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }
}