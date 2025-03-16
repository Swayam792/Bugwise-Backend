package com.swayam.bugwise.utils;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

@Slf4j
public class DTOConverter {
    private static final ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static <E, D> D convertToDTO(E entity, Class<D> dtoClass){
        if(entity == null){
            return null;
        }
        return modelMapper.map(entity, dtoClass);
    }

}
