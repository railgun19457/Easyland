package com.example.easyland.repository;

import com.example.easyland.domain.Land;
import org.bukkit.Chunk;

import java.util.List;
import java.util.Optional;

/**
 * 领地数据访问接口
 * 定义所有领地数据操作的抽象方法
 */
public interface LandRepository {

    /**
     * 保存或更新领地
     * @param land 领地对象
     * @return 保存后的领地（包含生成的ID）
     */
    Land save(Land land);

    /**
     * 批量保存或更新领地
     * @param lands 领地列表
     * @return 保存后的领地列表
     */
    List<Land> saveAll(List<Land> lands);

    /**
     * 根据数据库ID查找领地
     * @param id 数据库ID
     * @return 领地对象（如果存在）
     */
    Optional<Land> findById(Long id);

    /**
     * 根据领地ID查找领地
     * @param landId 领地ID（玩家设置的名称）
     * @return 领地对象（如果存在）
     */
    Optional<Land> findByLandId(String landId);

    /**
     * 根据所有者UUID查找所有领地
     * @param ownerUuid 所有者UUID
     * @return 领地列表
     */
    List<Land> findByOwner(String ownerUuid);

    /**
     * 根据世界名称查找所有领地
     * @param worldName 世界名称
     * @return 领地列表
     */
    List<Land> findByWorld(String worldName);

    /**
     * 查找包含指定区块的领地
     * @param chunk 区块
     * @return 领地对象（如果存在）
     */
    Optional<Land> findByChunk(Chunk chunk);

    /**
     * 获取所有已认领的领地
     * @return 领地列表
     */
    List<Land> findAllClaimed();

    /**
     * 获取所有未认领的领地
     * @return 领地列表
     */
    List<Land> findAllUnclaimed();

    /**
     * 获取所有领地
     * @return 领地列表
     */
    List<Land> findAll();

    /**
     * 删除领地
     * @param id 数据库ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据领地ID删除领地
     * @param landId 领地ID
     * @return 是否删除成功
     */
    boolean deleteByLandId(String landId);

    /**
     * 统计玩家拥有的领地数量
     * @param ownerUuid 所有者UUID
     * @return 领地数量
     */
    int countByOwner(String ownerUuid);

    /**
     * 检查领地ID是否已存在
     * @param landId 领地ID
     * @return 是否存在
     */
    boolean existsByLandId(String landId);

    /**
     * 初始化数据存储（创建表、文件等）
     */
    void initialize();

    /**
     * 关闭数据存储连接
     */
    void close();
}
