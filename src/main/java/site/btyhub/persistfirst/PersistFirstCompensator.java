package site.btyhub.persistfirst;

/**
 *
 * @author: baotingyu
 * @date: 2023/12/9
 **/
public interface PersistFirstCompensator {

    void compensate();

    void compensate(Integer id);

}
