package com.lzj.search.repository;

import com.lzj.search.entity.Subway;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by lizijian.
 */
public interface SubwayRepository extends CrudRepository<Subway, Long>{

    List<Subway> findAllByCityEnName(String cityEnName);
}
