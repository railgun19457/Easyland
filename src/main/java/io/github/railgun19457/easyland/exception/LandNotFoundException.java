package io.github.railgun19457.easyland.exception;

/**
 * 当尝试访问不存在的领地时抛出的异常。
 */
public class LandNotFoundException extends Exception {
    
    private final String landId;
    
    /**
     * 构造一个新的LandNotFoundException。
     *
     * @param landId 不存在的领地ID
     */
    public LandNotFoundException(String landId) {
        super("ID为'" + landId + "'的领地不存在");
        this.landId = landId;
    }
    
    /**
     * 获取导致异常的领地ID。
     *
     * @return 领地ID
     */
    public String getLandId() {
        return landId;
    }
}